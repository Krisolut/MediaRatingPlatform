package app.router;

import app.security.AuthMiddleware;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Router implements HttpHandler {
    private final Map<String, Map<String, RouteDefinition>> routes = new HashMap<>();
    private final AuthMiddleware authMiddleware;

    public Router(AuthMiddleware authMiddleware) {
        this.authMiddleware = Objects.requireNonNull(authMiddleware, "authMiddleware");
    }

    public void register(String method, String path, RouteHandler handler, boolean requiresAuth) {
        routes.computeIfAbsent(path, ignored -> new HashMap<>())
              .put(method.toUpperCase(), new RouteDefinition(handler, requiresAuth));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        Map<String, RouteDefinition> methodMap = routes.get(path);
        if (methodMap == null) {
            JsonUtil.sendError(exchange, 404, "Resource Not Found", "RESSOURCE_NOT_FOUND");
            return;
        }

        RouteDefinition routeDefinition = methodMap.get(method);
        if(routeDefinition == null) {
            JsonUtil.sendError(exchange, 405, "Method not allowed", "METHOD_NOT_ALLOWED");
            return;
        }

        RouteHandler handler = routeDefinition.handler();
        try {
            if (routeDefinition.requiresAuth()) {
                authMiddleware.handle(exchange, handler);
            } else {
                handler.handle(exchange);
            }
        } catch (Exception ex) {
            JsonUtil.sendError(exchange, 500, "Internal Server Error", "INTERNAL_SERVER_ERROR");
        }
    }

    public interface RouteHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record RouteDefinition(RouteHandler handler, boolean requiresAuth){ }

}
