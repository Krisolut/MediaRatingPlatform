package app.controller;

import app.dto.RatingDto;

import app.model.MediaEntry;
import app.model.Rating;
import app.security.AuthMiddleware;
import app.service.MediaService;
import app.service.RatingService;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import java.util.Optional;
import java.util.List;

public class RatingController {
    private final RatingService ratingService;
    private final MediaService mediaService;

    public RatingController(RatingService ratingService, MediaService mediaService) {
        this.ratingService = ratingService;
        this.mediaService = mediaService;
    }

    public void create(HttpExchange exchange) throws IOException {
        if (!JsonUtil.isJsonRequest(exchange)) {
            JsonUtil.sendError(exchange, 415, "Content-Type must be JSON", "UNSUPPORTED_MEDIA_TYPE");
            return;
        }
        RatingInput input;
        try {
            input = JsonUtil.readJson(exchange.getRequestBody(), RatingInput.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }

        String userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        if (userId == null) {
            JsonUtil.sendError(exchange, 401, "Unauthorized", "UNAUTHORIZED");
            return;
        }
        if (input.mediaId == null || input.mediaId.isBlank() || input.stars == null) {
            JsonUtil.sendError(exchange, 400, "mediaId and stars are required", "BAD_REQUEST");
            return;
        }
        if (input.stars < 1 || input.stars > 5) {
            JsonUtil.sendError(exchange, 400, "stars must be between 1 and 5", "BAD_REQUEST");
            return;
        }
        Optional<MediaEntry> mediaEntry = mediaService.findById(input.mediaId);
        if (mediaEntry.isEmpty()) {
            JsonUtil.sendError(exchange, 404, "Media not found", "NOT_FOUND");
            return;
        }
        try {
            var created = ratingService.create(input.mediaId, userId, input.stars, input.comment, mediaEntry.get());
            if (created.isEmpty()) {
                JsonUtil.sendError(exchange, 400, "Invalid rating data", "BAD_REQUEST");
                return;
            }
            JsonUtil.sendJsonResponse(exchange, 201, toDto(created.get()));
        } catch (RatingService.DuplicateRatingException ex) {
            JsonUtil.sendError(exchange, 409, ex.getMessage(), "CONFLICT");
        }
    }

    public void listByMedia(HttpExchange exchange) throws IOException {
        String mediaId = extractQueryParameter(exchange.getRequestURI().getRawQuery(), "mediaId");
        if (mediaId == null || mediaId.isBlank()) {
            JsonUtil.sendError(exchange, 400, "mediaId query parameter is required", "BAD_REQUEST");
            return;
        }
        List<RatingDto> ratings = ratingService.findByMediaId(mediaId).stream()
                .map(this::toDto)
                .toList();
        JsonUtil.sendJsonResponse(exchange, 200, ratings);
    }

    private String extractQueryParameter(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private RatingDto toDto(Rating rating) {
        String createdAtIso = DateTimeFormatter.ISO_INSTANT.format(rating.getCreatedAt());
        return new RatingDto(
                rating.getId(),
                rating.getMediaId(),
                rating.getUserId(),
                rating.getStars(),
                rating.getComment(),
                createdAtIso
        );
    }

    public record RatingInput (
            String mediaId,
            Integer stars,
            String comment
    )
    {}
}
