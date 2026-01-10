package app.service;

import app.model.MediaEntry;
import app.model.Rating;
import app.model.User;
import app.model.enums.AgeRestriction;
import app.repo.*;
import app.repo.sql.*;
import app.util.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest {
    private UserRepository userRepository;
    private TokenRepository tokenRepository;
    private MediaRepository mediaRepository;
    private RatingRepository ratingRepository;
    private FavoriteRepository favoriteRepository;
    private RatingLikeRepository likeRepository;
    private Database database;

    private MediaService mediaService;
    private RatingService ratingService;
    private FavoriteService favoriteService;
    private RecommendationService recommendationService;
    private LeaderboardService leaderboardService;
    private UserService userService;

    @BeforeEach
    void setup() {
        database = new Database();
        try {
            database.initializeSchema(Path.of("db/schema.sql"));
            clearTables();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        userRepository = new SqlUserRepository(database);
        tokenRepository = new SqlTokenRepository(database);
        mediaRepository = new SqlMediaRepository(database);
        ratingRepository = new SqlRatingRepository(database);
        favoriteRepository = new SqlFavoriteRepository(database);
        likeRepository = new SqlRatingLikeRepository(database);

        mediaService = new MediaService(mediaRepository);
        ratingService = new RatingService(ratingRepository, likeRepository, mediaService);
        favoriteService = new FavoriteService(favoriteRepository, mediaRepository);
        recommendationService = new RecommendationService(mediaRepository, ratingRepository);
        leaderboardService = new LeaderboardService(userRepository, ratingRepository);
        userService = new UserService(userRepository);
    }

    private void clearTables() throws SQLException {
        try (Connection conn = database.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE rating_likes RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE ratings RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE favorites RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE media_entries RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE tokens RESTART IDENTITY CASCADE");
        }
    }

    private User createUser(String name) {
        User user = new User(0L, name, "hash", null, null, 0, 0.0, java.time.Instant.now());
        return userRepository.save(user);
    }

    private MediaEntry createMedia(String title, long owner) {
        return mediaService.create(title, "desc", "MOVIE", 2024, AgeRestriction.FSK12, List.of("Action"), owner).orElseThrow();
    }

    @Test
    void registerAndLogin() {
        AuthService authService = new AuthService(userRepository, tokenRepository, new app.security.BCryptPasswordHasher(), new app.security.JwtService());
        assertTrue(authService.register("alice", "password").isPresent());
        assertTrue(authService.login("alice", "password").isPresent());
        assertTrue(authService.login("alice", "wrong").isEmpty());
    }

    @Test
    void mediaOwnerCannotBeOverwritten() {
        User owner = createUser("owner");
        MediaEntry media = createMedia("Film", owner.getId());
        assertThrows(MediaService.UnauthorizedException.class, () -> mediaService.update(media.getId(), owner.getId() + 1, "new", null, null, null, null));
    }

    @Test
    void ratingUpsertUpdatesAverage() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        Rating updated = ratingService.upsertRating(media.getId(), user.getId(), 3, "ok");
        assertEquals(3, updated.getStars());
        assertEquals(3, mediaRepository.findById(media.getId()).get().getAverageRating());
    }

    @Test
    void confirmSetsFlag() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        Rating rating = ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        Rating confirmed = ratingService.confirm(rating.getId(), user.getId()).orElseThrow();
        assertTrue(confirmed.isConfirmed());
    }

    @Test
    void deleteRatingUpdatesAggregation() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        ratingService.delete(ratingRepository.findAll().get(0).getId(), user.getId());
        assertEquals(0, mediaRepository.findById(media.getId()).get().getRatingCount());
    }

    @Test
    void likePreventsDuplicates() {
        User author = createUser("a");
        User liker = createUser("b");
        MediaEntry media = createMedia("Film", author.getId());
        Rating rating = ratingService.upsertRating(media.getId(), author.getId(), 5, "great");
        assertTrue(ratingService.like(rating.getId(), liker.getId()).isPresent());
        assertTrue(ratingService.like(rating.getId(), liker.getId()).isEmpty());
    }

    @Test
    void favoriteAddRemove() {
        User user = createUser("u");
        MediaEntry media = createMedia("Film", user.getId());
        assertTrue(favoriteService.markFavorite(user.getId(), media.getId()).isPresent());
        assertTrue(favoriteService.removeFavorite(user.getId(), media.getId()));
    }

    @Test
    void genreRecommendationUsesHighlyRatedItems() {
        User user = createUser("u");
        MediaEntry media1 = mediaService.create("Film1", "d", "MOVIE", 2020, AgeRestriction.FSK12, List.of("Action"), user.getId()).orElseThrow();
        MediaEntry media2 = mediaService.create("Film2", "d", "MOVIE", 2021, AgeRestriction.FSK12, List.of("Drama"), user.getId()).orElseThrow();
        ratingService.upsertRating(media1.getId(), user.getId(), 5, "great");
        var recs = recommendationService.recommendByGenre(user.getId());
        assertTrue(recs.stream().anyMatch(m -> m.getId() == media1.getId()));
    }

    @Test
    void leaderboardOrdersByRatingCount() {
        User u1 = createUser("u1");
        User u2 = createUser("u2");
        MediaEntry media = createMedia("Film", u1.getId());
        ratingService.upsertRating(media.getId(), u1.getId(), 4, "nice");
        ratingService.upsertRating(media.getId(), u2.getId(), 5, "great");
        ratingService.upsertRating(media.getId(), u2.getId(), 4, "update");
        var board = leaderboardService.mostActiveUsers();
        assertEquals(u2.getId(), board.get(0).getId());
    }

    @Test
    void profileUpdateChangesFields() {
        User user = createUser("u1");
        var updated = userService.updateProfile(user.getId(), "mail@example.com", "Action").orElseThrow();
        assertEquals("Action", updated.getFavoriteGenre());
    }

    @Test
    void unauthorizedRatingUpdateThrows() {
        User user = createUser("u1");
        User other = createUser("u2");
        MediaEntry media = createMedia("Film", user.getId());
        Rating rating = ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        assertThrows(RatingService.UnauthorizedException.class, () -> ratingService.updateRating(rating.getId(), other.getId(), 3, "bad"));
    }

    @Test
    void mediaFilterByRating() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        var query = new MediaService.MediaQuery(null, null, null, null, null, 4.0, "score");
        var list = mediaService.findAll(query);
        assertEquals(1, list.size());
    }

    @Test
    void favoritesListReturnsMedia() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        favoriteService.markFavorite(user.getId(), media.getId());
        assertEquals(media.getId(), favoriteService.listFavorites(user.getId()).get(0).getId());
    }

    @Test
    void recommendationContentConsidersType() {
        User user = createUser("u1");
        MediaEntry media1 = mediaService.create("Film1", "d", "MOVIE", 2020, AgeRestriction.FSK12, List.of("Action"), user.getId()).orElseThrow();
        MediaEntry media2 = mediaService.create("Game1", "d", "GAME", 2020, AgeRestriction.FSK12, List.of("Action"), user.getId()).orElseThrow();
        ratingService.upsertRating(media2.getId(), user.getId(), 5, "great");
        var recs = recommendationService.recommendByContent(user.getId());
        assertEquals(media2.getId(), recs.get(0).getId());
    }

    @Test
    void ratingDeletionByOtherFails() {
        User u1 = createUser("u1");
        User u2 = createUser("u2");
        MediaEntry media = createMedia("Film", u1.getId());
        Rating rating = ratingService.upsertRating(media.getId(), u1.getId(), 4, "ok");
        assertThrows(RatingService.UnauthorizedException.class, () -> ratingService.delete(rating.getId(), u2.getId()));
    }

    @Test
    void duplicateFavoriteReturnsEmpty() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        assertTrue(favoriteService.markFavorite(user.getId(), media.getId()).isPresent());
        assertTrue(favoriteService.markFavorite(user.getId(), media.getId()).isEmpty());
    }

    @Test
    void confirmOtherUsersRatingFails() {
        User u1 = createUser("u1");
        User u2 = createUser("u2");
        MediaEntry media = createMedia("Film", u1.getId());
        Rating rating = ratingService.upsertRating(media.getId(), u1.getId(), 5, "great");
        assertThrows(RatingService.UnauthorizedException.class, () -> ratingService.confirm(rating.getId(), u2.getId()));
    }

    @Test
    void mediaQueryFiltersGenre() {
        User u1 = createUser("u1");
        createMedia("Film1", u1.getId());
        mediaService.create("Drama", "d", "MOVIE", 2020, AgeRestriction.FSK12, List.of("Drama"), u1.getId());
        var query = new MediaService.MediaQuery(null, "Drama", null, null, null, null, null);
        var list = mediaService.findAll(query);
        assertEquals(1, list.size());
    }

    @Test
    void userStatisticsUpdatedFromRatings() {
        User user = createUser("u1");
        MediaEntry media = createMedia("Film", user.getId());
        ratingService.upsertRating(media.getId(), user.getId(), 4, "good");
        ratingService.upsertRating(media.getId(), user.getId(), 5, "great");
        User updated = userService.findByIdWithStats(user.getId()).orElseThrow();
        assertEquals(1, updated.getTotalRatings());
        assertEquals(5.0, updated.getAverageGivenRating());
    }

    @Test
    void leaderboardHandlesEmpty() {
        assertTrue(leaderboardService.mostActiveUsers().isEmpty());
    }
}