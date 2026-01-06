package app.router;

import app.controller.AuthController;
import app.controller.MediaController;
import app.controller.RatingController;
import app.controller.UserController;

public final class Routes {
    private Routes() {}

    public static void registerRoutes(Router router, AuthController authController,
                                      MediaController mediaController, RatingController ratingController, UserController userController) {
        router.register("POST", "/api/users/register", authController::register, false);
        router.register("POST", "/api/users/login", authController::login, false);

        router.register("GET", "/api/media", mediaController::list, false);
        router.register("POST", "/api/media", mediaController::create, true);
        router.register("GET", "/api/media/{mediaId}", mediaController::details, false);
        router.register("PUT", "/api/media/{mediaId}", mediaController::update, true);
        router.register("DELETE", "/api/media/{mediaId}", mediaController::delete, true);
        router.register("POST", "/api/media/{mediaId}/rate", mediaController::rate, true);
        router.register("POST", "/api/media/{mediaId}/favorite", mediaController::favorite, true);
        router.register("DELETE", "/api/media/{mediaId}/favorite", mediaController::unfavorite, true);

        router.register("PUT", "/api/ratings/{ratingId}", ratingController::update, true);
        router.register("DELETE", "/api/ratings/{ratingId}", ratingController::delete, true);
        router.register("POST", "/api/ratings/{ratingId}/like", ratingController::like, true);
        router.register("POST", "/api/ratings/{ratingId}/confirm", ratingController::confirm, true);

        router.register("GET", "/api/users/{userId}/profile", userController::profile, true);
        router.register("PUT", "/api/users/{userId}/profile", userController::updateProfile, true);
        router.register("GET", "/api/users/{userId}/favorites", userController::favorites, true);
        router.register("GET", "/api/users/{userId}/recommendations", userController::recommendations, true);
        router.register("GET", "/api/leaderboard", userController::leaderboard, false);
    }
}