package app.router;

import app.security.AuthMiddleware;
import app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router implements HttpHandler {
    private final List<RouteDefinition> routes = new ArrayList<>();
    private final AuthMiddleware authMiddleware;

    public Router(AuthMiddleware authMiddleware) {
        this.authMiddleware = Objects.requireNonNull(authMiddleware, "authMiddleware");
    }

    public void register(String method, String path, RouteHandler handler, boolean requiresAuth) {
        RouteDefinition def = new RouteDefinition(method.toUpperCase(), path, buildPattern(path), extractParams(path), handler, requiresAuth);
        routes.add(def);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        RouteMatch match = findRoute(method, path);
        if (match == null) {
            JsonUtil.sendError(exchange, 404, "Resource Not Found", "NOT_FOUND");
            return;
        }

        match.applyPathParams(exchange);
        try {
            if (match.definition.requiresAuth()) {
                authMiddleware.handle(exchange, match.definition.handler());
            } else {
                match.definition.handler().handle(exchange);
            }
        } catch (Exception ex) {
            JsonUtil.sendError(exchange, 500, "Internal Server Error", "INTERNAL_SERVER_ERROR");
        }
    }

    private RouteMatch findRoute(String method, String path) {
        for (RouteDefinition def : routes) {
            Matcher matcher = def.pattern.matcher(path);
            if (def.method.equals(method) && matcher.matches()) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < def.paramNames.size(); i++) {
                    params.put(def.paramNames.get(i), matcher.group(i + 1));
                }
                return new RouteMatch(def, params);
            }
        }
        return null;
    }

    private Pattern buildPattern(String path) {
        String regex = path.replaceAll("\\{([^/]+)}", "([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    private List<String> extractParams(String path) {
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^/]+)}").matcher(path);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    public interface RouteHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record RouteDefinition(String method, String rawPath, Pattern pattern, List<String> paramNames,
                                   RouteHandler handler, boolean requiresAuth){ }

    private record RouteMatch(RouteDefinition definition, Map<String, String> params) {
        void applyPathParams(HttpExchange exchange) {
            params.forEach((k, v) -> exchange.setAttribute("pathParam:" + k, v));
        }
    }
}