package nz.ac.wgtn.swen301.restappender.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class StatsCSVServlet extends HttpServlet {
    private static final String HEADER_ROW = "logger\tALL\tTRACE\tDEBUG\tINFO\tWARN\tERROR\tFATAL\tOFF\n";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, int[]> stats = StatsUtils.calculateLogStats();
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER_ROW);
        for (Map.Entry<String, int[]> loggerEntry : stats.entrySet()) {
            csvBuilder.append(loggerEntry.getKey());
            for (int count : loggerEntry.getValue()) {
                csvBuilder.append("\t").append(count);
            }
            csvBuilder.append("\n");
        }

        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(csvBuilder.toString());
    }
}
