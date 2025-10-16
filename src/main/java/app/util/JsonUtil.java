package app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.time.Instant;
import java.util.HashMap;

public final class JsonUtil {
    public static final String APPLICATION_JSON = "application/json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static{
        // Optik
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }
    private JsonUtil(){}

    // Serialisiert ein Objekt in einen JSON-String
    public static <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
        return MAPPER.readValue(inputStream, clazz);
    }

    public static void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = serialize(body);
        exchange.getResponseGeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        } finally {
            exchange.close();
        }
    }

    // Hilfsmethode für Antworten ohne Body (z.B. Delete, No Content)
    public static void sendEmptyResponse(HttpExchange exchange, int statusCode) throws IOException {
        exchange,getResponseHeaders().set("Content-Type", APPLICATION_JSON + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, -1); // -1 indicates no body
        exchange.close();
    }

    // Fehlermeldungen
    public static void sendError(HttpExchange, int statusCode, String message, String code) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("code", code);
        error.put("timestamp", Instant.now().toString());
        sendJsonResponse(exchange, statusCode, error);
    }

    private static byte[] serialize(Object body) throws JsonProcessingException {
        if (body instanceof byte[]) {
            return (byte[]) body;
        }
        String json = MAPPER.writeValueAsString(body);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
