package nz.ac.wgtn.swen301.restappender.server;

import com.google.gson.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LogEvent {
    private static final List<String> PROPERTIES = List.of("id", "message", "timestamp", "thread", "logger", "level");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static final JsonSerializer<LogEvent> JSON_SERIALIZER = (logEntry, t, j) -> {
        JsonObject json = new JsonObject();
        json.addProperty("id", logEntry.getId().toString());
        json.addProperty("message", logEntry.getMessage());
        json.addProperty("timestamp", logEntry.getTimestamp().format(TIMESTAMP_FORMATTER));
        json.addProperty("thread", logEntry.getThread());
        json.addProperty("logger", logEntry.getLogger());
        json.addProperty("level", logEntry.getLevel().toString());
        if (logEntry.getErrorDetails() != null) {
            json.addProperty("errorDetails", logEntry.getErrorDetails());
        }
        return json;
    };

    public static final JsonDeserializer<LogEvent> JSON_DESERIALIZER = (jsonElement, t, j) -> {
        JsonObject json = jsonElement.getAsJsonObject();
        for (String requiredProperty : PROPERTIES) {
            if (!json.has(requiredProperty)) {
                throw new JsonParseException("Missing property: " + requiredProperty);
            }
        }
        UUID id;
        try {
            id = UUID.fromString(json.get("id").getAsString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid id (required format: uuid)");
        }
        LogLevel level;
        try {
            level = LogLevel.fromString(json.get("level").getAsString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid level");
        }
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(json.get("timestamp").getAsString(), TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new JsonParseException("Invalid timestamp");
        }
        String message = json.get("message").getAsString();
        String thread = json.get("thread").getAsString();
        String logger = json.get("logger").getAsString();
        String errorDetails = json.has("errorDetails") ? json.get("errorDetails").getAsString() : null;
        return new LogEvent(id, message, timestamp, thread, logger, level, errorDetails);
    };

    public enum LogLevel {
        ALL(0), TRACE(1), DEBUG(2), INFO(3), WARN(4), ERROR(5), FATAL(6), OFF(7);

        private final int severity;

        LogLevel(int severity) {
            this.severity = severity;
        }

        public static LogLevel fromString(String levelStr) throws IllegalArgumentException {
            return LogLevel.valueOf(levelStr.toUpperCase());
        }

        public boolean isAtLeastLevel(LogLevel level) {
            return this.severity >= level.severity;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private final UUID id;
    private final String message;
    private final LocalDateTime timestamp;
    private final String thread;
    private final String logger;
    private final LogLevel level;
    private final String errorDetails;

    public LogEvent(UUID id, String message, LocalDateTime timestamp, String thread, String logger, LogLevel level, String errorDetails) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.thread = thread;
        this.logger = logger;
        this.level = level;
        this.errorDetails = errorDetails;
    }

    public boolean isAtLeastLevel(LogLevel level) {
        return this.level.isAtLeastLevel(level);
    }

    public UUID getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getThread() {
        return thread;
    }

    public String getLogger() {
        return logger;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent logEvent = (LogEvent) o;
        return id.equals(logEvent.getId())
                && message.equals(logEvent.getMessage())
                && timestamp.equals(logEvent.getTimestamp())
                && thread.equals(logEvent.getThread())
                && logger.equals(logEvent.getLogger())
                && level == logEvent.getLevel()
                && Objects.equals(errorDetails, logEvent.getErrorDetails());
    }
}
