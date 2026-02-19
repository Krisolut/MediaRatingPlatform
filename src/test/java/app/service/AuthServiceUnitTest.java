package app.service;

import app.model.User;
import app.repo.TokenRepository;
import app.repo.UserRepository;
import app.security.JwtService;
import app.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceUnitTest {

    @Test
    void registerRejectsBlankUsername() {
        AuthService service = new AuthService(new FakeUserRepository(), new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertTrue(service.register("   ", "pw").isEmpty());
    }

    @Test
    void registerRejectsBlankPassword() {
        AuthService service = new AuthService(new FakeUserRepository(), new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertTrue(service.register("alice", "  ").isEmpty());
    }

    @Test
    void registerStoresTrimmedUsernameAndHashedPassword() {
        FakeUserRepository userRepo = new FakeUserRepository();
        FakePasswordHasher hasher = new FakePasswordHasher();
        AuthService service = new AuthService(userRepo, new FakeTokenRepository(), hasher, new FakeJwtService());

        User user = service.register("  alice  ", "secret").orElseThrow();

        assertEquals("alice", user.getUsername());
        assertEquals("HASH_secret", user.getPasswordHash());
        assertEquals(1, userRepo.findAll().size());
    }

    @Test
    void registerThrowsForDuplicateUsername() {
        FakeUserRepository userRepo = new FakeUserRepository();
        userRepo.save(new User(0, "alice", "h", null, null, 0, 0, Instant.now()));
        AuthService service = new AuthService(userRepo, new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertThrows(AuthService.DuplicateUserException.class, () -> service.register("alice", "secret"));
    }

    @Test
    void loginReturnsEmptyWhenUserNotFound() {
        AuthService service = new AuthService(new FakeUserRepository(), new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertTrue(service.login("ghost", "pw").isEmpty());
    }

    @Test
    void loginReturnsEmptyWhenPasswordDoesNotMatch() {
        FakeUserRepository userRepo = new FakeUserRepository();
        userRepo.save(new User(0, "alice", "HASH_secret", null, null, 0, 0, Instant.now()));
        AuthService service = new AuthService(userRepo, new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertTrue(service.login("alice", "wrong").isEmpty());
    }

    @Test
    void loginStoresTokenAndReturnsResult() {
        FakeUserRepository userRepo = new FakeUserRepository();
        User saved = userRepo.save(new User(0, "alice", "HASH_secret", null, null, 0, 0, Instant.now()));
        FakeTokenRepository tokenRepo = new FakeTokenRepository();
        AuthService service = new AuthService(userRepo, tokenRepo, new FakePasswordHasher(), new FakeJwtService());

        AuthService.AuthResult result = service.login("alice", "secret").orElseThrow();

        assertEquals(saved.getId(), result.getUser().getId());
        assertEquals("token-" + saved.getId(), result.getToken());
        assertEquals(saved.getId(), tokenRepo.findUserIdByToken(result.getToken()).orElseThrow());
    }

    @Test
    void validateTokenDelegatesToRepository() {
        FakeTokenRepository tokenRepo = new FakeTokenRepository();
        tokenRepo.storeToken("abc", 42L);
        AuthService service = new AuthService(new FakeUserRepository(), tokenRepo, new FakePasswordHasher(), new FakeJwtService());

        assertEquals(42L, service.validateToken("abc").orElseThrow());
        assertTrue(service.validateToken("missing").isEmpty());
    }

    @Test
    void getUserByIdReturnsNullWhenMissing() {
        AuthService service = new AuthService(new FakeUserRepository(), new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        assertNull(service.getUserById(999L));
    }

    @Test
    void getUserByIdReturnsUserWhenFound() {
        FakeUserRepository userRepo = new FakeUserRepository();
        User saved = userRepo.save(new User(0, "alice", "h", null, null, 0, 0, Instant.now()));
        AuthService service = new AuthService(userRepo, new FakeTokenRepository(), new FakePasswordHasher(), new FakeJwtService());

        User found = service.getUserById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
    }

    private static final class FakePasswordHasher implements PasswordHasher {
        @Override
        public String hash(String plainText) {
            return "HASH_" + plainText;
        }

        @Override
        public boolean matches(String plainText, String hash) {
            return Objects.equals(hash(plainText), hash);
        }
    }

    private static final class FakeJwtService extends JwtService {
        @Override
        public String generateToken(String userId) {
            return "token-" + userId;
        }
    }

    private static final class FakeTokenRepository implements TokenRepository {
        private final Map<String, Long> tokens = new HashMap<>();

        @Override
        public void storeToken(String token, long userId) {
            tokens.put(token, userId);
        }

        @Override
        public Optional<Long> findUserIdByToken(String token) {
            return Optional.ofNullable(tokens.get(token));
        }

        @Override
        public void revoke(String token) {
            tokens.remove(token);
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<Long, User> usersById = new LinkedHashMap<>();
        private long seq = 1;

        @Override
        public Optional<User> findByUsername(String username) {
            return usersById.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public Optional<User> findByIdWithStats(long id) {
            return findById(id);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersById.values());
        }

        @Override
        public List<User> findAllWithStats() {
            return findAll();
        }

        @Override
        public User save(User user) {
            long id = user.getId() == 0 ? seq++ : user.getId();
            User stored = new User(id, user.getUsername(), user.getPasswordHash(), user.getEmail(), user.getFavoriteGenre(),
                    user.getTotalRatings(), user.getAverageGivenRating(), user.getCreatedAt());
            usersById.put(id, stored);
            return stored;
        }

        @Override
        public User update(User user) {
            usersById.put(user.getId(), user);
            return user;
        }
    }
}
