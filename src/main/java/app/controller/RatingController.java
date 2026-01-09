package app.controller;

import app.dto.RatingRequest;
import app.model.Rating;
import app.service.RatingService;
import app.security.AuthMiddleware;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void update(HttpExchange exchange) throws IOException {
        long ratingId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:ratingId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);

        if (!JsonUtil.requireJson(exchange)) return;

        RatingRequest payload;
        try {
            payload = JsonUtil.readJson(exchange.getRequestBody(), RatingRequest.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }
        try {
            Optional<Rating> updated = ratingService.updateRating(ratingId, userId, payload.stars(), payload.comment());
            if (updated.isEmpty()) {
                JsonUtil.sendError(exchange, 404, "Rating not found", "NOT_FOUND");
                return;
            }
            JsonUtil.sendJsonResponse(exchange, 200, updated.get());
        } catch (RatingService.UnauthorizedException ex) {
            JsonUtil.sendError(exchange, 403, ex.getMessage(), "FORBIDDEN");
        }
    }

    public void delete(HttpExchange exchange) throws IOException {
        long ratingId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:ratingId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        try {
            boolean deleted = ratingService.delete(ratingId, userId);
            if (!deleted) {
                JsonUtil.sendError(exchange, 404, "Rating not found", "NOT_FOUND");
                return;
            }
            JsonUtil.sendEmptyResponse(exchange, 204);
        } catch (RatingService.UnauthorizedException ex) {
            JsonUtil.sendError(exchange, 403, ex.getMessage(), "FORBIDDEN");
        }
    }

    public void like(HttpExchange exchange) throws IOException {
        long ratingId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:ratingId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        var like = ratingService.like(ratingId, userId);
        if (like.isEmpty()) {
            JsonUtil.sendError(exchange, 409, "Cannot like rating", "CONFLICT");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 201, like.get());
    }

    public void confirm(HttpExchange exchange) throws IOException {
        long ratingId = Long.parseLong(String.valueOf(exchange.getAttribute("pathParam:ratingId")));
        Long userId = AuthMiddleware.getAuthenticatedUserId(exchange);
        try {
            Optional<Rating> confirmed = ratingService.confirm(ratingId, userId);
            if (confirmed.isEmpty()) {
                JsonUtil.sendError(exchange, 404, "Rating not found", "NOT_FOUND");
                return;
            }
            JsonUtil.sendJsonResponse(exchange, 200, confirmed.get());
        } catch (RatingService.UnauthorizedException ex) {
            JsonUtil.sendError(exchange, 403, ex.getMessage(), "FORBIDDEN");
        }
    }

}