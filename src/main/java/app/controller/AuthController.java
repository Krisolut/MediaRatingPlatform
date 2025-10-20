package app.controller;

import app.model.User;
import app.service.AuthService;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    public void register(HttpExchange exchange) throws IOException {
        if (!JsonUtil.isJsonRequest(exchange)) {
            JsonUtil.sendError(exchange, 415, "Content-Type must be JSON", "UNSUPPORTED_MEDIA_TYPE");
            return;
        }
        UserCredentials credentials;
        try {
            credentials = JsonUtil.readJson(exchange.getRequestBody(), UserCredentials.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }
        try {
            var registered = authService.register(credentials.username, credentials.password);
            if(registered.isEmpty()) {
                JsonUtil.sendError(exchange, 400, "Invalid credentials", "INVALID_CREDENTIALS");
                return;
            }
            User user = registered.get();
            JsonUtil.sendJsonResponse(exchange, 201, toUserDto(user));
        } catch (AuthService.DuplicateUserException ex){
            JsonUtil.sendError(exchange, 409, ex.getMessage(), "CONFLICT");
        }
    }

    public void login(HttpExchange exchange) throws IOException {
        if(!JsonUtil.isJsonRequest(exchange)) {
            JsonUtil.sendError(exchange, 415, "Content-Type must be JSON", "UNSUPPORTED_MEDIA_TYPE");
            return;
        }

        UserCredentials credentials;
        try {
            credentials = JsonUtil.readJson(exchange.getRequestBody(), UserCredentials.class);
        } catch (IOException ex) {
            JsonUtil.sendError(exchange, 400, "Invalid JSON", "BAD_REQUEST");
            return;
        }
        var result = authService.login(credentials.username, credentials.password);
        if (result.isEmpty()) {
            JsonUtil.sendError(exchange, 401, "Invalid username or password", "UNAUTHORIZED");
            return;
        }
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("token", result.get().getToken());
        JsonUtil.sendJsonResponse(exchange, 200, tokenResponse);
    }
    /**
     * Ausgelagert in JsonUtil
    private boolean isJsonRequest(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.startsWith(JsonUtil.APPLICATION_JSON);
    }
     **/
    private Map<String, Object> toUserDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("displayName", user.getDisplayName());
        dto.put("createdAt", user.getCreatedAt().toString());
        return dto;
    }

    public static class UserCredentials {
        public String username;
        public String password;
    }
}
