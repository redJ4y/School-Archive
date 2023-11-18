package nz.ac.wgtn.swen301.restappender.server;

import com.google.gson.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPostLogs {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_SERIALIZER)
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_DESERIALIZER)
            .create();

    @BeforeEach
    public void resetPersistency() {
        Persistency.DB.clear();
    }

    static Stream<LogEvent> validLogEventsProvider() {
        return Stream.of(
                new LogEvent(UUID.randomUUID(), "Valid log 1", LocalDateTime.parse("12-10-2023 14:10:00", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.DEBUG, null),
                new LogEvent(UUID.randomUUID(), "Valid log 2", LocalDateTime.parse("12-10-2023 11:27:05", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.INFO, null),
                new LogEvent(UUID.randomUUID(), "Valid log 3", LocalDateTime.parse("12-10-2023 17:54:30", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.WARN, "testErrorDetails"),
                new LogEvent(UUID.randomUUID(), "Valid log 4", LocalDateTime.parse("12-10-2023 09:18:15", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.ERROR, "testErrorDetails"),
                new LogEvent(UUID.randomUUID(), "Valid log 5", LocalDateTime.parse("12-10-2023 20:45:50", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.FATAL, "testErrorDetails"),
                new LogEvent(UUID.randomUUID(), "Valid log 6", LocalDateTime.parse("12-10-2023 07:33:40", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.TRACE, null),
                new LogEvent(UUID.randomUUID(), "Valid log 7", LocalDateTime.parse("12-10-2023 08:41:45", TIMESTAMP_FORMATTER), "main", "restappender.server.TestPostLogs", LogEvent.LogLevel.OFF, null)
        );
    }

    @ParameterizedTest
    @MethodSource("validLogEventsProvider")
    public void testAddLogEvent(LogEvent logEvent) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(GSON.toJson(logEvent).getBytes());
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doPost(request, response);

        assertEquals(201, response.getStatus());
        assertEquals(logEvent, Persistency.DB.get(0));
    }

    @Test
    public void testAddMultipleLogEvents() throws IOException {
        LogsServlet service = new LogsServlet();
        List<LogEvent> logEvents = validLogEventsProvider().collect(Collectors.toList());
        for (LogEvent logEvent : logEvents) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent(GSON.toJson(logEvent).getBytes());
            request.setContentType("application/json");
            MockHttpServletResponse response = new MockHttpServletResponse();
            service.doPost(request, response);

            assertEquals(201, response.getStatus());
        }

        assertEquals(logEvents.size(), Persistency.DB.size());
        for (int i = 0; i < logEvents.size(); i++) {
            assertEquals(logEvents.get(i), Persistency.DB.get(i));
        }
    }

    @ParameterizedTest
    @MethodSource("validLogEventsProvider")
    public void testAddLogEventConflict(LogEvent logEvent) throws IOException {
        List<LogEvent> allLogEvents = validLogEventsProvider().collect(Collectors.toList());
        Persistency.DB.addAll(allLogEvents);
        Persistency.DB.add(Math.abs(logEvent.hashCode()) % allLogEvents.size(), logEvent);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(GSON.toJson(logEvent).getBytes());
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doPost(request, response);

        assertEquals(409, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Missing all properties:
            "{}",
            // Invalid id:
            "{\"id\":\"12345\", \"message\":\"msg\", \"timestamp\":\"12-10-2023 12:00:00\", \"thread\":\"main\", \"logger\":\"logger\", \"level\":\"info\"}",
            // Invalid level:
            "{\"id\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"message\":\"msg\", \"timestamp\":\"12-10-2023 12:00:00\", \"thread\":\"main\", \"logger\":\"logger\", \"level\":\"invalid\"}",
            // Invalid timestamp:
            "{\"id\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"message\":\"msg\", \"timestamp\":\"invalid\", \"thread\":\"main\", \"logger\":\"logger\", \"level\":\"info\"}",
            // Missing id:
            "{\"message\":\"msg\", \"timestamp\":\"12-10-2023 12:00:00\", \"thread\":\"main\", \"logger\":\"logger\", \"level\":\"info\"}",
            // Missing message:
            "{\"id\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"timestamp\":\"12-10-2023 12:00:00\", \"thread\":\"main\", \"logger\":\"logger\", \"level\":\"info\"}",
    })
    public void testAddInvalidLogEvent(String invalidJson) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(invalidJson.getBytes());
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doPost(request, response);

        assertEquals(400, response.getStatus());
    }
}
