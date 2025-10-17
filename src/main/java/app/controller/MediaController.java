package app.controller;

import app.model.MediaEntry;
import app.model.enums.MediaType;
import app.model.enums.ageRestriction;
import app.security.AuthMiddleware;
import app.service.MediaService;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) { this.mediaService = mediaService; }

    public void list(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> media = mediaService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        JsonUtil.sendJsonResponse(exchange, 200, media);
    }

    public void create(HttpExchange exchange) throws IOException {
        if (!isJsonRequest(exchange)) {
            JsonUtil.sendError(exchange, 415, "Content-Type must be JSON", "UNSUPPORTED_MEDIA_TYPE");
            return;
        }
        MediaInput input;
        try {
            input = JsonUtil.readJson(exchange.getRequestBody(), MediaInput.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }
        if (input.title == null || input.title.isBlank() || input.type == null) {
            JsonUtil.sendError(exchange, 400, "Title and type are required", "BAD_REQUEST");
            return;
        }
        String userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        var created = mediaService.create(input.title, input.type.name(), input.releaseYear, input.fsk, userId);
        if (created.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Invalid media data", "BAD_REQUEST");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 201, toDto(created.get()));
    }

    private boolean isJsonRequest(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.contains("application/json");
    }
    private Map<String, Object> toDto(MediaEntry entry) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", entry.getId());
        dto.put("title", entry.getTitle());
        dto.put("type", entry.getType().name());
        dto.put("releaseYear", entry.getReleaseYear());
        dto.put("fsk", entry.getFsk() != null ? entry.getFsk().name() : null);
        dto.put("createdByUserId", entry.getCreatedByUserId());
        dto.put("createdAt", entry.getCreatedAt().toString());
        return dto;
    }

    public static class MediaInput {
        public String title;
        public MediaType type;
        public Integer releaseYear;
        public ageRestriction fsk;
    }
}
