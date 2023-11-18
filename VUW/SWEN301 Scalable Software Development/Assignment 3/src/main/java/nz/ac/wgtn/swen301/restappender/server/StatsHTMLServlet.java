package nz.ac.wgtn.swen301.restappender.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class StatsHTMLServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, int[]> stats = StatsUtils.calculateLogStats();
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html>");
        htmlBuilder.append("<head><title>Log Statistics</title></head>");
        htmlBuilder.append("<body>");
        htmlBuilder.append("<table border=\"1\">");
        htmlBuilder.append("<tr><th>logger</th>");
        for (LogEvent.LogLevel level : LogEvent.LogLevel.values()) {
            htmlBuilder.append("<th>").append(level.name()).append("</th>");
        }
        htmlBuilder.append("</tr>");
        for (Map.Entry<String, int[]> loggerEntry : stats.entrySet()) {
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append(loggerEntry.getKey()).append("</td>");
            for (int count : loggerEntry.getValue()) {
                htmlBuilder.append("<td>").append(count).append("</td>");
            }
            htmlBuilder.append("</tr>");
        }
        htmlBuilder.append("</table>");
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(htmlBuilder.toString());
    }
}
