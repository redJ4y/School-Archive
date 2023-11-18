package nz.ac.wgtn.swen301.restappender.server;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class StatsExcelServlet extends HttpServlet {
    private static final String[] HEADER_ROW = new String[]{"logger", "ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF"};

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, int[]> stats = StatsUtils.calculateLogStats();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("stats");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADER_ROW.length; i++) {
            headerRow.createCell(i).setCellValue(HEADER_ROW[i]);
        }
        int rowIndex = 1;
        for (Map.Entry<String, int[]> loggerEntry : stats.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(loggerEntry.getKey());
            int[] counts = loggerEntry.getValue();
            for (int i = 0; i < counts.length; i++) {
                row.createCell(i + 1).setCellValue(counts[i]);
            }
        }

        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setCharacterEncoding("UTF-8");
        workbook.write(resp.getOutputStream());
        workbook.close();
    }
}
