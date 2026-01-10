package app.controller;

import app.dto.ProfileUpdateRequest;
import app.security.AuthMiddleware;
import app.service.FavoriteService;
import app.service.LeaderboardService;
import app.service.RecommendationService;
import app.service.UserService;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class UserController {
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final RecommendationService recommendationService;
    private final LeaderboardService leaderboardService;

    public UserController(UserService userService, FavoriteService favoriteService,
                          RecommendationService recommendationService, LeaderboardService leaderboardService) {
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.recommendationService = recommendationService;
        this.leaderboardService = leaderboardService;
    }

    public void profile(HttpExchange exchange) throws IOException {
        Long userId = requireUserId(exchange);
        if (userId == null) return;
        var user = userService.findByIdWithStats(userId);
        if (user.isEmpty()) {
            JsonUtil.sendError(exchange, 404, "User not found", "NOT_FOUND");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 200, user.get());
    }

    public void updateProfile(HttpExchange exchange) throws IOException {
        Long userId = requireUserId(exchange);
        if (userId == null) return;

        Long requester = AuthMiddleware.getAuthenticatedUserId(exchange);
        if (requester == null) {
            JsonUtil.sendError(exchange, 401, "Authentication required", "UNAUTHORIZED");
            return;
        }
        if (!userId.equals(requester)) {
            JsonUtil.sendError(exchange, 403, "Cannot edit other users", "FORBIDDEN");
            return;
        }

        if (!JsonUtil.requireJson(exchange)) return;

        ProfileUpdateRequest payload;
        try {
            payload = JsonUtil.readJson(exchange.getRequestBody(), ProfileUpdateRequest.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }
        var updated = userService.updateProfile(userId, payload.email(), payload.favoriteGenre());
        if (updated.isEmpty()) {
            JsonUtil.sendError(exchange, 404, "User not found", "NOT_FOUND");
            return;
        }
        JsonUtil.sendJsonResponse(exchange, 200, updated.get());
    }

    public void favorites(HttpExchange exchange) throws IOException {
        Long userId = requireUserId(exchange);
        if (userId == null) return;
        JsonUtil.sendJsonResponse(exchange, 200, favoriteService.listFavorites(userId));
    }

    public void recommendations(HttpExchange exchange) throws IOException {
        Long userId = requireUserId(exchange);
        if (userId == null) return;
        String type = exchange.getRequestURI().getQuery();
        String value = "genre";
        if (type != null && type.contains("content")) {
            value = "content";
        }
        if ("content".equalsIgnoreCase(value)) {
            JsonUtil.sendJsonResponse(exchange, 200, recommendationService.recommendByContent(userId));
        } else {
            JsonUtil.sendJsonResponse(exchange, 200, recommendationService.recommendByGenre(userId));
        }
    }

    public void leaderboard(HttpExchange exchange) throws IOException {
        JsonUtil.sendJsonResponse(exchange, 200, leaderboardService.mostActiveUsers());
    }

    private Long requireUserId(HttpExchange exchange) throws IOException {
        Object raw = exchange.getAttribute("pathParam:userId");
        if (raw == null) {
            JsonUtil.sendError(exchange, 400, "Missing userId", "BAD_REQUEST");
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid userId", "BAD_REQUEST");
            return null;
        }
    }
}