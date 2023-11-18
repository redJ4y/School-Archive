package nz.ac.wgtn.swen301.restappender.server;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGetLogs {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_SERIALIZER)
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_DESERIALIZER)
            .create();
    private static final Type LOG_EVENT_LIST_TYPE = new TypeToken<List<LogEvent>>() {
    }.getType();

    @BeforeEach
    public void setupPersistency() {
        Persistency.DB.clear();
        addLog(LogEvent.LogLevel.DEBUG, LocalDateTime.parse("12-10-2023 14:10:00", TIMESTAMP_FORMATTER), null);
        addLog(LogEvent.LogLevel.INFO, LocalDateTime.parse("12-10-2023 11:27:05", TIMESTAMP_FORMATTER), null);
        addLog(LogEvent.LogLevel.WARN, LocalDateTime.parse("12-10-2023 17:54:30", TIMESTAMP_FORMATTER), "testErrorDetails");
        addLog(LogEvent.LogLevel.ERROR, LocalDateTime.parse("12-10-2023 09:18:15", TIMESTAMP_FORMATTER), "testErrorDetails");
        addLog(LogEvent.LogLevel.FATAL, LocalDateTime.parse("12-10-2023 20:45:50", TIMESTAMP_FORMATTER), "testErrorDetails");
        addLog(LogEvent.LogLevel.TRACE, LocalDateTime.parse("12-10-2023 07:33:40", TIMESTAMP_FORMATTER), null);
        addLog(LogEvent.LogLevel.OFF, LocalDateTime.parse("12-10-2023 08:41:45", TIMESTAMP_FORMATTER), null);
    }

    private void addLog(LogEvent.LogLevel level, LocalDateTime timestamp, String errorDetails) {
        Persistency.DB.add(new LogEvent(
                UUID.randomUUID(),
                "Test log at level " + level,
                timestamp,
                "main",
                "restappender.server.TestGetLogs",
                level,
                errorDetails
        ));
    }

    @Test
    public void testAllLevels() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("limit", String.valueOf(Integer.MAX_VALUE));
        request.addParameter("level", "all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        List<LogEvent> returnedLogs = GSON.fromJson(response.getContentAsString(), LOG_EVENT_LIST_TYPE);
        assertEquals(Persistency.DB.size(), returnedLogs.size());
    }

    @Test
    public void testEachLogLevel() throws IOException {
        List<String> levels = List.of("debug", "info", "warn", "error", "fatal", "trace", "off");
        LogsServlet service = new LogsServlet();
        for (String level : levels) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter("limit", String.valueOf(Integer.MAX_VALUE));
            request.addParameter("level", level);
            MockHttpServletResponse response = new MockHttpServletResponse();
            service.doGet(request, response);

            assertEquals(200, response.getStatus());
            assertEquals("application/json", response.getContentType());
            List<LogEvent> returnedLogs = GSON.fromJson(response.getContentAsString(), LOG_EVENT_LIST_TYPE);
            for (LogEvent log : returnedLogs) {
                assertTrue(log.getLevel().isAtLeastLevel(LogEvent.LogLevel.fromString(level)));
            }
        }
    }

    @Test
    public void testLimitParameter() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("limit", "3");
        request.addParameter("level", "all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        List<LogEvent> returnedLogs = GSON.fromJson(response.getContentAsString(), LOG_EVENT_LIST_TYPE);
        assertEquals(3, returnedLogs.size());
        Persistency.DB.sort((log1, log2) -> log2.getTimestamp().compareTo(log1.getTimestamp()));
        assertEquals(Persistency.DB.get(0).getLevel(), returnedLogs.get(0).getLevel());
        assertEquals(Persistency.DB.get(1).getLevel(), returnedLogs.get(1).getLevel());
        assertEquals(Persistency.DB.get(2).getLevel(), returnedLogs.get(2).getLevel());
    }

    @Test
    public void testLogsOrderingByTimestamp() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("limit", String.valueOf(Integer.MAX_VALUE));
        request.addParameter("level", "all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        List<LogEvent> returnedLogs = GSON.fromJson(response.getContentAsString(), LOG_EVENT_LIST_TYPE);
        LocalDateTime lastTimestamp = null;
        for (LogEvent log : returnedLogs) {
            if (lastTimestamp != null) {
                assertTrue(lastTimestamp.isAfter(log.getTimestamp()));
            }
            lastTimestamp = log.getTimestamp();
        }
    }

    static Stream<Arguments> invalidParametersProvider() {
        return Stream.of(
                Arguments.of("1", "invalidLevel", 400),
                Arguments.of("-5", "debug", 400),
                Arguments.of("notAnInt", "debug", 400),
                Arguments.of("0", "debug", 400)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidParametersProvider")
    public void testInvalidParameters(String limit, String level, int expectedStatus) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("limit", limit);
        request.addParameter("level", level);
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(expectedStatus, response.getStatus());
    }

    @Test
    public void testMissingParameter() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("level", "all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testNoAvailableLogs() throws IOException {
        Persistency.DB.clear();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("limit", String.valueOf(Integer.MAX_VALUE));
        request.addParameter("level", "all");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        List<LogEvent> returnedLogs = GSON.fromJson(response.getContentAsString(), LOG_EVENT_LIST_TYPE);
        assertTrue(returnedLogs.isEmpty());
    }
}
