package app.service;

import app.model.*;
import app.model.enums.AgeRestriction;
import app.model.enums.MediaType;
import app.repo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UnitServiceTest {

    @Test
    void mediaCreateRejectsInvalidType() {
        var repo = new FakeMediaRepository();
        var service = new MediaService(repo);

        var created = service.create("Title", "desc", "INVALID", 2024, AgeRestriction.FSK12, List.of("Action"), 1L);

        assertTrue(created.isEmpty());
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void mediaCreateTrimsTitleAndStoresEntity() {
        var repo = new FakeMediaRepository();
        var service = new MediaService(repo);

        var created = service.create("  My Film  ", "desc", "movie", 2024, AgeRestriction.FSK12, List.of("Action"), 2L).orElseThrow();

        assertEquals("My Film", created.getTitle());
        assertEquals(MediaType.MOVIE, created.getType());
        assertEquals(1, repo.findAll().size());
    }

    @Test
    void mediaUpdateThrowsForNonOwner() {
        var repo = new FakeMediaRepository();
        var media = repo.save(new MediaEntry(0, "Film", "d", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of("Action"), 5L, Instant.now(), 0, 0));
        var service = new MediaService(repo);

        assertThrows(MediaService.UnauthorizedException.class,
                () -> service.update(media.getId(), 99L, "new", null, null, null, null));
    }

    @Test
    void mediaFindAllFiltersByGenreCaseInsensitive() {
        var repo = new FakeMediaRepository();
        repo.save(new MediaEntry(0, "A", "", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of("Action"), 1L, Instant.now(), 4.5, 2));
        repo.save(new MediaEntry(0, "B", "", MediaType.GAME, 2023, AgeRestriction.FSK16, List.of("Drama"), 1L, Instant.now(), 3.0, 1));
        var service = new MediaService(repo);

        var filtered = service.findAll(new MediaService.MediaQuery(null, "action", null, null, null, null, null));

        assertEquals(1, filtered.size());
        assertEquals("A", filtered.get(0).getTitle());
    }

    @Test
    void ratingUpsertRecomputesMediaAggregates() {
        var mediaRepo = new FakeMediaRepository();
        var media = mediaRepo.save(new MediaEntry(0, "Film", "", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of("Action"), 1L, Instant.now(), 0, 0));
        var mediaService = new MediaService(mediaRepo);
        var ratingRepo = new FakeRatingRepository();
        var likeRepo = new FakeRatingLikeRepository();
        var service = new RatingService(ratingRepo, likeRepo, mediaService);

        service.upsertRating(media.getId(), 10L, 5, "great");
        service.upsertRating(media.getId(), 11L, 3, "ok");

        var updated = mediaRepo.findById(media.getId()).orElseThrow();
        assertEquals(2, updated.getRatingCount());
        assertEquals(4.0, updated.getAverageRating());
    }

    @Test
    void ratingLikeRejectsOwnRating() {
        var mediaRepo = new FakeMediaRepository();
        var media = mediaRepo.save(new MediaEntry(0, "Film", "", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of(), 1L, Instant.now(), 0, 0));
        var ratingRepo = new FakeRatingRepository();
        var rating = ratingRepo.save(new Rating(0, media.getId(), 7L, 5, "great", false, Instant.now(), Instant.now(), 1));
        var service = new RatingService(ratingRepo, new FakeRatingLikeRepository(), new MediaService(mediaRepo));

        var like = service.like(rating.getId(), 7L);

        assertTrue(like.isEmpty());
    }

    @Test
    void favoriteListReturnsOnlyExistingMedia() {
        var favoriteRepo = new FakeFavoriteRepository();
        var mediaRepo = new FakeMediaRepository();
        var favoriteService = new FavoriteService(favoriteRepo, mediaRepo);

        favoriteRepo.save(new Favorite(0, 1L, 999L));

        assertTrue(favoriteService.listFavorites(1L).isEmpty());
    }

    @Test
    void userUpdateProfileReturnsEmptyWhenUserMissing() {
        var service = new UserService(new FakeUserRepository());

        var updated = service.updateProfile(100L, "mail@example.com", "Action");

        assertTrue(updated.isEmpty());
    }

    @Test
    void recommendationByGenreUsesHighRatings() {
        var mediaRepo = new FakeMediaRepository();
        var m1 = mediaRepo.save(new MediaEntry(0, "Film", "", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of("Action"), 1, Instant.now(), 4.2, 4));
        mediaRepo.save(new MediaEntry(0, "Drama", "", MediaType.MOVIE, 2024, AgeRestriction.FSK12, List.of("Drama"), 1, Instant.now(), 4.0, 3));
        var ratingRepo = new FakeRatingRepository();
        ratingRepo.save(new Rating(0, m1.getId(), 44L, 5, "great", false, Instant.now(), Instant.now(), 1));
        var service = new RecommendationService(mediaRepo, ratingRepo);

        var recs = service.recommendByGenre(44L);

        assertEquals(1, recs.size());
        assertEquals("Film", recs.get(0).getTitle());
    }

    @Test
    void leaderboardSortsByActivityCountThenLastActivity() {
        var userRepo = new FakeUserRepository();
        var u1 = userRepo.save(new User(0, "u1", "h", null, null, 0, 0, Instant.now()));
        var u2 = userRepo.save(new User(0, "u2", "h", null, null, 0, 0, Instant.now()));
        var ratingRepo = new FakeRatingRepository();
        ratingRepo.save(new Rating(0, 1, u1.getId(), 4, "", false, Instant.now(), Instant.parse("2024-01-01T00:00:00Z"), 1));
        ratingRepo.save(new Rating(0, 1, u2.getId(), 4, "", false, Instant.now(), Instant.parse("2024-01-03T00:00:00Z"), 1));
        ratingRepo.save(new Rating(0, 2, u2.getId(), 5, "", false, Instant.now(), Instant.parse("2024-01-02T00:00:00Z"), 1));

        var board = new LeaderboardService(userRepo, ratingRepo).mostActiveUsers();

        assertEquals(2, board.size());
        assertEquals(u2.getId(), board.get(0).getId());
    }

    private static final class FakeMediaRepository implements MediaRepository {
        private final Map<Long, MediaEntry> storage = new LinkedHashMap<>();
        private long seq = 1;

        public MediaEntry save(MediaEntry mediaEntry) {
            long id = mediaEntry.getId() == 0 ? seq++ : mediaEntry.getId();
            MediaEntry stored = new MediaEntry(id, mediaEntry.getTitle(), mediaEntry.getDescription(), mediaEntry.getType(), mediaEntry.getReleaseYear(),
                    mediaEntry.getAgeRestriction(), mediaEntry.getGenres(), mediaEntry.getCreatedByUserId(), mediaEntry.getCreatedAt(), mediaEntry.getAverageRating(), mediaEntry.getRatingCount());
            storage.put(id, stored);
            return stored;
        }

        public MediaEntry update(MediaEntry entry) { storage.put(entry.getId(), entry); return entry; }
        public boolean delete(long id) { return storage.remove(id) != null; }
        public List<MediaEntry> findAll() { return new ArrayList<>(storage.values()); }
        public Optional<MediaEntry> findById(long id) { return Optional.ofNullable(storage.get(id)); }
    }

    private static final class FakeRatingRepository implements RatingRepository {
        private final Map<Long, Rating> storage = new LinkedHashMap<>();
        private long seq = 1;

        public Rating save(Rating rating) {
            long id = rating.getId() == 0 ? seq++ : rating.getId();
            Rating stored = new Rating(id, rating.getMediaId(), rating.getUserId(), rating.getStars(), rating.getComment(), rating.isConfirmed(), rating.getCreatedAt(), rating.getUpdatedAt(), rating.getActivityCount());
            storage.put(id, stored);
            return stored;
        }

        public Rating update(Rating rating) { storage.put(rating.getId(), rating); return rating; }
        public boolean delete(long ratingId) { return storage.remove(ratingId) != null; }
        public List<Rating> findByMediaId(long mediaId) { return storage.values().stream().filter(r -> r.getMediaId() == mediaId).toList(); }
        public Optional<Rating> findByUserIdAndMediaId(long userId, long mediaId) { return storage.values().stream().filter(r -> r.getUserId() == userId && r.getMediaId() == mediaId).findFirst(); }
        public Optional<Rating> findById(long ratingId) { return Optional.ofNullable(storage.get(ratingId)); }
        public List<Rating> findByUserId(long userId) { return storage.values().stream().filter(r -> r.getUserId() == userId).toList(); }
        public List<Rating> findAll() { return new ArrayList<>(storage.values()); }
    }

    private static final class FakeRatingLikeRepository implements RatingLikeRepository {
        private final List<RatingLike> likes = new ArrayList<>();
        private long seq = 1;

        public RatingLike save(RatingLike like) {
            RatingLike stored = new RatingLike(seq++, like.getRatingId(), like.getUserId());
            likes.add(stored);
            return stored;
        }

        public Optional<RatingLike> findByUserAndRating(long userId, long ratingId) {
            return likes.stream().filter(l -> l.getUserId() == userId && l.getRatingId() == ratingId).findFirst();
        }

        public List<RatingLike> findByRating(long ratingId) { return likes.stream().filter(l -> l.getRatingId() == ratingId).toList(); }
    }

    private static final class FakeFavoriteRepository implements FavoriteRepository {
        private final List<Favorite> favorites = new ArrayList<>();
        private long seq = 1;

        public Favorite save(Favorite favorite) {
            Favorite stored = new Favorite(seq++, favorite.getUserId(), favorite.getMediaId());
            favorites.add(stored);
            return stored;
        }

        public boolean delete(long userId, long mediaId) {
            return favorites.removeIf(f -> f.getUserId() == userId && f.getMediaId() == mediaId);
        }

        public Optional<Favorite> findByUserAndMedia(long userId, long mediaId) {
            return favorites.stream().filter(f -> f.getUserId() == userId && f.getMediaId() == mediaId).findFirst();
        }

        public List<Favorite> findByUserId(long userId) { return favorites.stream().filter(f -> f.getUserId() == userId).toList(); }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<Long, User> users = new LinkedHashMap<>();
        private long seq = 1;

        public Optional<User> findByUsername(String username) { return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst(); }
        public Optional<User> findById(long id) { return Optional.ofNullable(users.get(id)); }
        public Optional<User> findByIdWithStats(long id) { return Optional.ofNullable(users.get(id)); }
        public List<User> findAll() { return new ArrayList<>(users.values()); }
        public List<User> findAllWithStats() { return new ArrayList<>(users.values()); }
        public User save(User user) {
            long id = user.getId() == 0 ? seq++ : user.getId();
            User stored = new User(id, user.getUsername(), user.getPasswordHash(), user.getEmail(), user.getFavoriteGenre(), user.getTotalRatings(), user.getAverageGivenRating(), user.getCreatedAt());
            users.put(id, stored);
            return stored;
        }
        public User update(User user) { users.put(user.getId(), user); return user; }
    }
}
