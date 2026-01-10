package app;

import app.controller.AuthController;
import app.controller.MediaController;
import app.controller.RatingController;
import app.controller.UserController;
import app.repo.*;
import app.repo.sql.*;
import app.router.Router;
import app.router.Routes;
import app.security.AuthMiddleware;
import app.security.BCryptPasswordHasher;
import app.security.JwtService;
import app.security.PasswordHasher;
import app.service.*;
import app.util.Database;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.file.Path;
import java.net.InetSocketAddress;

public class App {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        new App().start();
    }

    public void start() throws IOException {
        Database database = new Database();
        try {
            database.initializeSchema(Path.of("db/schema.sql"));
            try (var connection = database.getConnection()) {
                if (!connection.isValid(2)) {
                    throw new IOException("Database connection is not valid");
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to initialize database schema", e);
        }

        UserRepository userRepository = new SqlUserRepository(database);
        TokenRepository tokenRepository = new SqlTokenRepository(database);
        MediaRepository mediaRepository = new SqlMediaRepository(database);
        RatingRepository ratingRepository = new SqlRatingRepository(database);
        FavoriteRepository favoriteRepository = new SqlFavoriteRepository(database);
        RatingLikeRepository likeRepository = new SqlRatingLikeRepository(database);

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        JwtService jwtService = new JwtService();

        MediaService mediaService = new MediaService(mediaRepository);
        RatingService ratingService = new RatingService(ratingRepository, likeRepository, mediaService);
        AuthService authService = new AuthService(userRepository, tokenRepository, passwordHasher, jwtService);
        FavoriteService favoriteService = new FavoriteService(favoriteRepository, mediaRepository);
        RecommendationService recommendationService = new RecommendationService(mediaRepository, ratingRepository);
        LeaderboardService leaderboardService = new LeaderboardService(userRepository, ratingRepository);
        UserService userService = new UserService(userRepository);

        AuthController authController = new AuthController(authService);
        MediaController mediaController = new MediaController(mediaService, ratingService, favoriteService);
        RatingController ratingController = new RatingController(ratingService);
        UserController userController = new UserController(userService, favoriteService, recommendationService, leaderboardService);

        AuthMiddleware authMiddleware = new AuthMiddleware(jwtService, tokenRepository);
        Router router = new Router(authMiddleware);
        Routes.registerRoutes(router, authController, mediaController, ratingController, userController);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api", router);
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down HTTP server");
            server.stop(1);
        }));
    }

}