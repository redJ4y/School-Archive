package nz.ac.wgtn.swen301.assignment2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class JsonLayout extends Layout {

    private static final Gson gson = new Gson();

    @Override
    public String format(LoggingEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("name", event.getLoggerName());
        json.addProperty("level", event.getLevel().toString());
        json.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("Z")).format(Instant.ofEpochMilli(event.getTimeStamp())));
        json.addProperty("thread", event.getThreadName());
        json.addProperty("message", Objects.requireNonNullElse(event.getRenderedMessage(), "null"));
        return gson.toJson(json);
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    @Override
    public void activateOptions() {
        // No options to activate
    }
}
