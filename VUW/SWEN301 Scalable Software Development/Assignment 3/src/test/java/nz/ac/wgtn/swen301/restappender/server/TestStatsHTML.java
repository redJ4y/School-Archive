package nz.ac.wgtn.swen301.restappender.server;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestStatsHTML {
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
                "restappender.server.TestStatsHTML",
                level,
                null
        ));
    }

    @Test
    public void testValidHTML() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        StatsHTMLServlet service = new StatsHTMLServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        String htmlData = response.getContentAsString();
        assertNotNull(htmlData);
        assertFalse(htmlData.isEmpty());
        Document document = Jsoup.parse(htmlData);
        Elements rows = document.select("tr");
        assertEquals(2, rows.size());
        Elements headerCells = document.select("th");
        assertEquals(LogEvent.LogLevel.values().length + 1, headerCells.size());
        assertEquals("logger", headerCells.get(0).text());
        for (int i = 0; i < LogEvent.LogLevel.values().length; i++) {
            assertEquals(LogEvent.LogLevel.values()[i].name(), headerCells.get(i + 1).text());
        }

        assertEquals("restappender.server.TestStatsHTML", rows.get(1).child(0).text());
        assertEquals("0", rows.get(1).child(1).text());
        assertEquals("1", rows.get(1).child(2).text());
        assertEquals("1", rows.get(1).child(3).text());
        assertEquals("1", rows.get(1).child(4).text());
        assertEquals("2", rows.get(1).child(5).text());
        assertEquals("0", rows.get(1).child(6).text());
        assertEquals("1", rows.get(1).child(7).text());
        assertEquals("1", rows.get(1).child(8).text());
    }
}
