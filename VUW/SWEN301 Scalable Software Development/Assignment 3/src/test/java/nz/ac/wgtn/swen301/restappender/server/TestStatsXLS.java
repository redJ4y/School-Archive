package nz.ac.wgtn.swen301.restappender.server;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestStatsXLS {
    private static final String[] HEADER_ROW = new String[]{"logger", "ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF"};
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
                "restappender.server.TestStatsXLS",
                level,
                null
        ));
    }

    @Test
    public void testValidXLS() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        StatsExcelServlet service = new StatsExcelServlet();
        service.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            assertEquals("stats", sheet.getSheetName());
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            for (int i = 0; i < HEADER_ROW.length; i++) {
                assertEquals(HEADER_ROW[i], headerRow.getCell(i).getStringCellValue());
            }
            Row dataRow = sheet.getRow(1);
            assertNotNull(dataRow);

            assertEquals("restappender.server.TestStatsXLS", dataRow.getCell(0).getStringCellValue());
            assertEquals(0, (int) dataRow.getCell(1).getNumericCellValue());
            assertEquals(1, (int) dataRow.getCell(2).getNumericCellValue());
            assertEquals(1, (int) dataRow.getCell(3).getNumericCellValue());
            assertEquals(1, (int) dataRow.getCell(4).getNumericCellValue());
            assertEquals(2, (int) dataRow.getCell(5).getNumericCellValue());
            assertEquals(0, (int) dataRow.getCell(6).getNumericCellValue());
            assertEquals(1, (int) dataRow.getCell(7).getNumericCellValue());
            assertEquals(1, (int) dataRow.getCell(8).getNumericCellValue());
        }
    }
}
