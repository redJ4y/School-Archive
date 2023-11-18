package nz.ac.wgtn.swen301.restappender.server;

import com.google.gson.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LogsServlet extends HttpServlet {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_SERIALIZER)
            .registerTypeAdapter(LogEvent.class, LogEvent.JSON_DESERIALIZER)
            .create();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String levelStr = req.getParameter("level");
        String limitStr = req.getParameter("limit");
        if (levelStr == null || limitStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing level and/or limit parameter");
            return;
        }
        LogEvent.LogLevel level;
        try {
            level = LogEvent.LogLevel.fromString(levelStr);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid level parameter");
            return;
        }
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
            if (limit <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid limit parameter");
            return;
        }

        List<LogEvent> results = Persistency.DB.stream()
                .filter(log -> log.isAtLeastLevel(level))
                .sorted(Comparator.comparing(LogEvent::getTimestamp, Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());

        JsonArray jsonArray = GSON.toJsonTree(results).getAsJsonArray();
        resp.setContentType("application/json");
        resp.getWriter().write(jsonArray.toString());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LogEvent logEvent;
        try {
            logEvent = GSON.fromJson(req.getReader(), LogEvent.class);
        } catch (JsonParseException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        if (Persistency.DB.stream().anyMatch(entry -> entry.getId().equals(logEvent.getId()))) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "A log event with this id already exists");
            return;
        }
        Persistency.DB.add(logEvent);
        resp.setStatus(HttpServletResponse.SC_CREATED);
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        Persistency.DB.clear();
    }
}
