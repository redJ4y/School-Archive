package nz.ac.wgtn.swen301.assignment2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class MemAppenderTest {

    private static final String TEST_JSON_FILE = "test.json";

    private MemAppender memAppender;

    @BeforeEach
    public void setUp() {
        memAppender = new MemAppender();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(Paths.get(TEST_JSON_FILE));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("loggingEventProvider")
    public void shouldAppendLoggingEvent(LoggingEvent event, String displayName) {
        memAppender.append(event);
        List<LoggingEvent> currentLogs = memAppender.getCurrentLogs();
        Assertions.assertEquals(1, currentLogs.size());
        Assertions.assertEquals(event, currentLogs.get(0));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("loggingEventProvider")
    public void shouldExportToJson(LoggingEvent event, String displayName) throws Exception {
        memAppender.append(event);
        memAppender.exportToJSON(TEST_JSON_FILE);

        try (FileReader reader = new FileReader(TEST_JSON_FILE)) {
            JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
            Assertions.assertEquals(1, jsonArray.size());
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("loggingEventProvider")
    public void shouldDiscardExcessLogs(LoggingEvent event, String displayName) {
        memAppender.setMaxSize(1);
        memAppender.append(event);
        Assertions.assertEquals(0, memAppender.getDiscardedLogCount());

        memAppender.append(event);
        Assertions.assertEquals(1, memAppender.getDiscardedLogCount());
    }

    @Test
    public void shouldDiscardLogsWhenMaxSizeIsZero() {
        setAppendAndValidateLogs(0, 1, 0, 1);
    }

    @Test
    public void shouldDiscardLogsOnMaxSizeChange() {
        setAppendAndValidateLogs(5, 3, 3, 0);
        memAppender.setMaxSize(2);
        Assertions.assertEquals(2, memAppender.getCurrentLogs().size());
        Assertions.assertEquals(1, memAppender.getDiscardedLogCount());
    }

    @Test
    public void shouldExportStructuredJson() throws Exception {
        setAppendAndValidateLogs(5, 3, 3, 0);
        memAppender.exportToJSON(TEST_JSON_FILE);

        try (FileReader reader = new FileReader(TEST_JSON_FILE)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();

            Assertions.assertTrue(jsonObject.has("name"));
            Assertions.assertTrue(jsonObject.has("level"));
            Assertions.assertTrue(jsonObject.has("timestamp"));
            Assertions.assertTrue(jsonObject.has("thread"));
            Assertions.assertTrue(jsonObject.has("message"));
        }
    }

    @Test
    public void shouldHaveInitialProperties() {
        Assertions.assertEquals(1000, memAppender.getMaxSize());
        Assertions.assertEquals(0, memAppender.getDiscardedLogCount());
        Assertions.assertTrue(memAppender.getCurrentLogs().isEmpty());
    }

    @Test
    public void shouldAllowPropertyChanges() {
        memAppender.setName("NewName");
        Assertions.assertEquals("NewName", memAppender.getName());

        memAppender.setMaxSize(2000);
        Assertions.assertEquals(2000, memAppender.getMaxSize());
    }

    @Test
    public void shouldResetOnClose() {
        setAppendAndValidateLogs(4, 5, 4, 1);
        memAppender.close();
        Assertions.assertEquals(0, memAppender.getDiscardedLogCount());
        Assertions.assertTrue(memAppender.getCurrentLogs().isEmpty());
    }

    @Test
    public void shouldReturnUnmodifiableList() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            memAppender.getCurrentLogs().add(createLoggingEvent(Level.FATAL, "Fatal Test"));
        });
    }

    @Test
    public void shouldThrowOnNegativeMaxSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            memAppender.setMaxSize(-1);
        });
    }

    @Test
    public void shouldNotRequireLayout() {
        Assertions.assertFalse(memAppender.requiresLayout());
    }

    private void setAppendAndValidateLogs(int maxSize, int appendCount, int expectedLogCount, int expectedDiscardedLogCount) {
        memAppender.setMaxSize(maxSize);
        for (int i = 0; i < appendCount; i++) {
            memAppender.append(createLoggingEvent(Level.INFO, "Info Test " + i));
        }
        Assertions.assertEquals(expectedLogCount, memAppender.getCurrentLogs().size());
        Assertions.assertEquals(expectedDiscardedLogCount, memAppender.getDiscardedLogCount());
    }

    private static LoggingEvent createLoggingEvent(Level level, String message) {
        return new LoggingEvent("Test.Logger", Logger.getLogger("Test.Logger"), System.currentTimeMillis(), level, message, null);
    }

    static Stream<Arguments> loggingEventProvider() {
        Logger logger1 = Logger.getLogger(MemAppenderTest.class);
        Logger logger2 = Logger.getLogger("Different.Logger");
        return Stream.of(
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.MemAppenderTest", logger1, System.currentTimeMillis(), Level.INFO, "Test message", null), "INFO Test message"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.MemAppenderTest", logger1, System.currentTimeMillis(), Level.ERROR, null, null), "ERROR No Message"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.MemAppenderTest", logger1, System.currentTimeMillis(), Level.DEBUG, "Debug level", null), "DEBUG Debug level"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.MemAppenderTest", logger1, System.currentTimeMillis(), Level.WARN, "Warning message", null), "WARN Warning message"),
                Arguments.of(new LoggingEvent("Different.Logger", logger2, System.currentTimeMillis(), Level.INFO, "Different logger name", null), "INFO Different logger name"),
                Arguments.of(new LoggingEvent("Different.Logger", logger2, System.currentTimeMillis(), Level.FATAL, null, null), "FATAL No Message")
        );
    }
}
