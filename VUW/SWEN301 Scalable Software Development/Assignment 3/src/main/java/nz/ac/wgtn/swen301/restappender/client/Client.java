package nz.ac.wgtn.swen301.restappender.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nz.ac.wgtn.swen301.restappender.server.LogEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class Client {
    private static final String SERVICE_URL = "http://localhost:8080/restappender/stats/";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Client <type (csv|excel)> <fileName>");
            System.exit(1);
        }
        String type = args[0];
        String fileName = args[1];
        if (!"csv".equals(type) && !"excel".equals(type)) {
            System.err.println("Invalid type specified: only 'csv' and 'excel' are accepted");
            System.exit(1);
        }

        // Uncomment for manual testing:
        // postTestData();

        try {
            downloadData(type, fileName);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void downloadData(String type, String fileName) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVICE_URL + type))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (InputStream data = response.body()) {
                Files.copy(data, Paths.get(fileName));
            }
        } else {
            System.err.println("Server returned status code: " + response.statusCode());
            System.exit(1);
        }
    }

    private static void postTestData() {
        String endpointURL = "http://localhost:8080/restappender/logs";
        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        Gson gson = new GsonBuilder().registerTypeAdapter(LogEvent.class, LogEvent.JSON_SERIALIZER).create();
        List<LogEvent> logEvents = List.of(
                new LogEvent(UUID.randomUUID(), "Test log 1", LocalDateTime.parse("12-10-2023 14:10:00", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.DEBUG, null),
                new LogEvent(UUID.randomUUID(), "Test log 2", LocalDateTime.parse("12-10-2023 11:27:05", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.INFO, null),
                new LogEvent(UUID.randomUUID(), "Test log 3", LocalDateTime.parse("12-10-2023 17:54:30", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.WARN, null),
                new LogEvent(UUID.randomUUID(), "Test log 4", LocalDateTime.parse("12-10-2023 09:18:15", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.WARN, null),
                new LogEvent(UUID.randomUUID(), "Test log 5", LocalDateTime.parse("12-10-2023 20:45:50", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.FATAL, null),
                new LogEvent(UUID.randomUUID(), "Test log 6", LocalDateTime.parse("12-10-2023 07:33:40", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.TRACE, null),
                new LogEvent(UUID.randomUUID(), "Test log 7", LocalDateTime.parse("12-10-2023 08:41:45", timestampFormatter), "main", "restappender.client.TestLogs", LogEvent.LogLevel.OFF, null)
        );
        HttpClient client = HttpClient.newHttpClient();
        for (LogEvent logEvent : logEvents) {
            String jsonLogData = gson.toJson(logEvent);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonLogData))
                    .build();
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                System.err.println("Error POSTing test data: " + e.getMessage());
            }
        }
    }
}
