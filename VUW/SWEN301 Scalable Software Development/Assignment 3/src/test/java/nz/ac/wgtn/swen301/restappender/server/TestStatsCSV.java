package nz.ac.wgtn.swen301.restappender.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestStatsCSV {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @BeforeEach
    public void setupPersistency() {
        Persistency.DB.clear();
        addLog(LogEvent.LogLevel.DEBUG, LocalDateTime.parse("12-10-2023 14:10:00", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.INFO, LocalDateTime.parse("12-10-2023 11:27:05", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.WARN, LocalDateTime.parse("12-10-2023 17:54:30", TIMESTAMP_FORMATTER));
        addLog(LogEvent.LogLevel.WARN, LocalDateTime.parse("12-10-2023 09:18:15", TIMESTAMP_FORMATTER));
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
                "restappender.server.TestStatsCSV",
                level,
                null
        ));
    }

    @Test
    public void testValidCSV() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        StatsCSVServlet service = new StatsCSVServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("text/csv", response.getContentType());
        String csvData = response.getContentAsString();
        assertNotNull(csvData);
        assertFalse(csvData.isEmpty());
        String[] lines = csvData.split("\n");
        assertEquals(2, lines.length);
        String[][] cells = new String[lines.length][];
        for (int i = 0; i < lines.length; i++) {
            cells[i] = lines[i].split("\t");
            assertEquals(LogEvent.LogLevel.values().length + 1, cells[i].length);
        }
        assertEquals("logger", cells[0][0]);
        for (int i = 0; i < LogEvent.LogLevel.values().length; i++) {
            assertEquals(LogEvent.LogLevel.values()[i].name(), cells[0][i + 1]);
        }

        assertEquals("restappender.server.TestStatsCSV", cells[1][0]);
        assertEquals("0", cells[1][1]);
        assertEquals("1", cells[1][2]);
        assertEquals("1", cells[1][3]);
        assertEquals("1", cells[1][4]);
        assertEquals("2", cells[1][5]);
        assertEquals("0", cells[1][6]);
        assertEquals("1", cells[1][7]);
        assertEquals("1", cells[1][8]);
    }
}
