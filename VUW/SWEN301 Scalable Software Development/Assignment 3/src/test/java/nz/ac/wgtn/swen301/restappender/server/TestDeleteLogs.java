package nz.ac.wgtn.swen301.restappender.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class TestDeleteLogs {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @BeforeEach
    public void setupPersistency() {
        Persistency.DB.clear();
        addLog(LogEvent.LogLevel.DEBUG, LocalDateTime.parse("12-10-2023 14:10:00", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.INFO, LocalDateTime.parse("12-10-2023 11:27:05", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.WARN, LocalDateTime.parse("12-10-2023 17:54:30", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.ERROR, LocalDateTime.parse("12-10-2023 09:18:15", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.FATAL, LocalDateTime.parse("12-10-2023 20:45:50", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.TRACE, LocalDateTime.parse("12-10-2023 07:33:40", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.OFF, LocalDateTime.parse("12-10-2023 08:41:45", TIMESTAMP_FORMATTER));
    }

    private void addLog(LogEvent.LogLevel level, LocalDateTime timestamp) {
        Persistency.DB.add(new LogEvent(
                UUID.randomUUID(),
                "Test log at level " + level,
                timestamp,
                "main",
                "restappender.server.TestDeleteLogs",
                level,
                null
        ));
    }

    @Test
    public void testDelete() {
        assumeFalse(Persistency.DB.isEmpty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        LogsServlet service = new LogsServlet();
        service.doDelete(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(Persistency.DB.isEmpty());
    }
}
