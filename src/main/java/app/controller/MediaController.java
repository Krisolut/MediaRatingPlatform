package app.controller;

import app.dto.MediaRequest;
import app.dto.RatingRequest;
import app.model.MediaEntry;
import app.model.Rating;
import app.model.enums.AgeRestriction;
import app.service.FavoriteService;
import app.service.MediaService;
import app.service.RatingService;
import app.util.JsonUtil;
import app.security.AuthMiddleware;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MediaController {
    private final MediaService mediaService;
    private final RatingService ratingService;
    private final FavoriteService favoriteService;

    public MediaController(MediaService mediaService, RatingService ratingService, FavoriteService favoriteService) {
        this.mediaService = mediaService;
        this.ratingService = ratingService;
        this.favoriteService = favoriteService;
    }

    public void list(HttpExchange exchange) throws IOException {
        var query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        MediaService.MediaQuery mediaQuery = new MediaService.MediaQuery(
                params.get("title"),
                params.get("genre"),
                params.get("mediaType"),
                params.containsKey("releaseYear") ? Integer.valueOf(params.get("releaseYear")) : null,
                params.containsKey("ageRestriction") ? AgeRestriction.valueOf(params.get("ageRestriction")) : null,
                params.containsKey("rating") ? Double.valueOf(params.get("rating")) : null,
                params.get("sortBy")
        );
        JsonUtil.sendJsonResponse(exchange, 200, mediaService.findAll(mediaQuery));
    }

    public void create(HttpExchange exchange) throws IOException {
        MediaRequest req = JsonUtil.readJson(exchange.getRequestBody(), MediaRequest.class);
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        var created = mediaService.create(req.title(), req.description(), req.mediaType(), req.releaseYear(),
                req.ageRestriction(), req.genres(), userId == null ? 0L : userId);
        if (created.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Invalid media payload", "BAD_REQUEST");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 201, created.get());
    }

    public void details(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Optional<MediaEntry> media = mediaService.findById(mediaId);
        if (media.isEmpty()) {
            JsonUtil.sendError(exchange, 404, "Media not found", "NOT_FOUND");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 200, media.get());
    }

    public void update(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        MediaRequest req = JsonUtil.readJson(exchange.getRequestBody(), MediaRequest.class);
        try {
            var updated = mediaService.update(mediaId, userId, req.title(), req.description(), req.releaseYear(), req.ageRestriction(), req.genres());
            if (updated.isEmpty()) {
                JsonUtil.sendError(exchange, 404, "Media not found", "NOT_FOUND");
                return;
            }
            JsonUtil.sendJsonResponse(exchange, 200, updated.get());
        } catch (MediaService.UnauthorizedException ex) {
            JsonUtil.sendError(exchange, 403, ex.getMessage(), "FORBIDDEN");
        }
    }

    public void delete(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        try {
            boolean deleted = mediaService.delete(mediaId, userId);
            if (!deleted) {
                JsonUtil.sendError(exchange, 404, "Media not found", "NOT_FOUND");
                return;
            }
            JsonUtil.sendEmptyResponse(exchange, 204);
        } catch (MediaService.UnauthorizedException ex) {
            JsonUtil.sendError(exchange, 403, ex.getMessage(), "FORBIDDEN");
        }
    }

    public void rate(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        RatingRequest req = JsonUtil.readJson(exchange.getRequestBody(), RatingRequest.class);
        Rating rating = ratingService.upsertRating(mediaId, userId, req.stars(), req.comment());
        JsonUtil.sendJsonResponse(exchange, 200, rating);
    }

    public void favorite(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        Optional<?> result = favoriteService.markFavorite(userId, mediaId);
        if (result.isEmpty()) {
            JsonUtil.sendError(exchange, 409, "Already favorited", "CONFLICT");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 201, result.get());
    }

    public void unfavorite(HttpExchange exchange) throws IOException {
        long mediaId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:mediaId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        boolean removed = favoriteService.removeFavorite(userId, mediaId);
        if (!removed) {
            JsonUtil.sendError(exchange, 404, "Favorite not found", "NOT_FOUND");
            return;
        }
        JsonUtil.sendEmptyResponse(exchange, 204);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length == 2) params.put(kv[0], kv[1]);
        }
        return params;
    }

}