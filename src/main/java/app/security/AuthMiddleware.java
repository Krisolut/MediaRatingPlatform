package app.security;

import app.repo.TokenRepository;
import app.router.Router;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class AuthMiddleware {
    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    public AuthMiddleware(JwtService jwtService, TokenRepository tokenRepository) {
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
    }
    public void handle(HttpExchange exchange, Router.RouteHandler next) throws IOException {
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                JsonUtil.sendError(exchange, 401, "Missing Authorization header", "UNAUTHORIZED");
                return;
            }

            String[] segments = authHeader.split(" ");
            if(segments.length != 2 || !"Bearer".equalsIgnoreCase(segments[0])) {
                JsonUtil.sendError(exchange, 401, "Invalid Auth Header", "UNAUTHORIZED");
                return;
            }

            String token = segments[1].trim();
            if (token.isEmpty()) {
                JsonUtil.sendError(exchange, 401, "Invalid Auth Header", "UNAUTHORIZED");
                return;
            }

            String userId = jwtService.verifyToken(token);
            if (tokenRepository.findUserIdByToken(token).isEmpty()) {
                JsonUtil.sendError(exchange, 401, "Unknown or revoked token", "UNAUTHORIZED");
                return;
            }
            exchange.setAttribute(USER_ID_ATTRIBUTE, Long.parseLong(userId));
            next.handle(exchange);
        } catch (JwtService.TokenVerificationException ex) {
            JsonUtil.sendError(exchange, 401, ex.getMessage(), "UNAUTHORIZED");
        }
    }

    public static Long getAuthenticatedUserId(HttpExchange exchange) {
        Object attribute = exchange.getAttribute(USER_ID_ATTRIBUTE);
        return attribute instanceof Long ? (Long) attribute : null;
    }
}