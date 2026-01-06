package app.controller;

import app.dto.TokenResponse;
import app.dto.UserCredentials;
import app.dto.UserDto;
import app.model.User;
import app.service.AuthService;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    public void register(HttpExchange exchange) throws IOException {
        if (!isJsonRequest(exchange)) {
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
            var registered = authService.register(credentials.username(), credentials.password());
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
        if(!isJsonRequest(exchange)) {
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
        var result = authService.login(credentials.username(), credentials.password());
        if (result.isEmpty()) {
            JsonUtil.sendError(exchange, 401, "Invalid username or password", "UNAUTHORIZED");
            return;
        }
        TokenResponse tokenResponse = new TokenResponse(result.get().getToken(), result.get().getUser().getId());
        JsonUtil.sendJsonResponse(exchange, 200, tokenResponse);
    }

    private boolean isJsonRequest(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.startsWith(JsonUtil.APPLICATION_JSON);
    }
    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getCreatedAt().toString());
    }
}