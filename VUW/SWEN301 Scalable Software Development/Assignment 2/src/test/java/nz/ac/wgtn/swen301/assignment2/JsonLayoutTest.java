package nz.ac.wgtn.swen301.assignment2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonLayoutTest {

    static Stream<Arguments> loggingEventProvider() {
        Logger logger1 = Logger.getLogger(JsonLayoutTest.class);
        Logger logger2 = Logger.getLogger("Different.Logger");
        return Stream.of(
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.JsonLayoutTest", logger1, System.currentTimeMillis(), Level.INFO, "Test message", null), "INFO Test message"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.JsonLayoutTest", logger1, System.currentTimeMillis(), Level.ERROR, null, null), "ERROR No Message"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.JsonLayoutTest", logger1, System.currentTimeMillis(), Level.DEBUG, "Debug level", null), "DEBUG Debug level"),
                Arguments.of(new LoggingEvent("nz.ac.wgtn.swen301.assignment2.JsonLayoutTest", logger1, System.currentTimeMillis(), Level.WARN, "Warning message", null), "WARN Warning message"),
                Arguments.of(new LoggingEvent("Different.Logger", logger2, System.currentTimeMillis(), Level.INFO, "Different logger name", null), "INFO Different logger name"),
                Arguments.of(new LoggingEvent("Different.Logger", logger2, System.currentTimeMillis(), Level.FATAL, null, null), "FATAL No Message")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("loggingEventProvider")
    public void testLoggingEvents(LoggingEvent event, String displayName) {
        JsonObject jsonObject = JsonParser.parseString(new JsonLayout().format(event)).getAsJsonObject();

        assertEquals(event.getLoggerName(), jsonObject.get("name").getAsString());
        assertEquals(event.getLevel().toString(), jsonObject.get("level").getAsString());
        assertEquals(DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("Z")).format(Instant.ofEpochMilli(event.getTimeStamp())), jsonObject.get("timestamp").getAsString());
        assertEquals(event.getThreadName(), jsonObject.get("thread").getAsString());
        assertEquals(Objects.requireNonNullElse(event.getMessage(), "null").toString(), jsonObject.get("message").getAsString());
    }

    @Test
    public void shouldIgnoreThrowable() {
        Assertions.assertTrue(new JsonLayout().ignoresThrowable());
    }
}
