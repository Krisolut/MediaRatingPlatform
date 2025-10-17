package app;

import app.controller.AuthController;
import app.controller.MediaController;
import app.controller.RatingController;
import app.repo.MediaRepository;
import app.repo.RatingRepository;
import app.repo.UserRepository;
import app.service.AuthService;
import app.service.MediaService;
import app.service.RatingService;
import app.repo.memo.InMemoryMediaRepository;
import app.repo.memo.InMemoryRatingRepository;
import app.repo.memo.InMemoryUserRepository;

import app.router.Router;

import app.security.AuthMiddleware;
import app.security.JwtService;
import app.security.BCryptPasswordHasher;
import app.security.PasswordHasher;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class App {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        new App().start();
    }

    public void start() throws IOException {
        UserRepository userRepository = new InMemoryUserRepository();
        MediaRepository mediaRepository = new InMemoryMediaRepository();
        RatingRepository ratingRepository = new InMemoryRatingRepository();

        PasswordHasher passwordHasher = new BCryptPasswordHasher();
        JwtService jwtService = new JwtService();
        AuthService authService = new AuthService(userRepository, passwordHasher, jwtService);
        MediaService mediaService = new MediaService(mediaRepository);
        RatingService ratingService = new RatingService(ratingRepository);

        AuthController authController = new AuthController(authService);
        MediaController mediaController = new MediaController(mediaService);
        RatingController ratingController = new RatingController(ratingService, mediaService);

        AuthMiddleware authMiddleware = new AuthMiddleware(jwtService);
        Router router = new Router(authMiddleware);
        registerRoutes(router, authController, mediaController, ratingController);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api", router);
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    private void registerRoutes(Router router, AuthController authController,
                                MediaController mediaController, RatingController ratingController) {
        router.register("POST", "/api/users/register", authController::register, false);
        router.register("POST", "/api/users/login", authController::login, false);
        router.register("GET", "/api/media", mediaController::list, false);
        router.register("POST", "/api/media", mediaController::create, true);
        router.register("POST", "/api/ratings", ratingController::create, true);
        router.register("GET", "/api/ratings", ratingController::listByMedia, true);
    }
}
