package app.service;

import app.model.User;
import app.repo.TokenRepository;
import app.repo.UserRepository;
import app.security.JwtService;
import app.security.PasswordHasher;

import java.time.Instant;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository, PasswordHasher passwordHasher, JwtService jwtService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    public Optional<User> register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new DuplicateUserException("Username already exists");
        }
        String passwordHash = passwordHasher.hash(password);
        User user = new User(0L, username.trim(), passwordHash, null, null, 0, 0.0, Instant.now());
        return Optional.of(userRepository.save(user));
    }

    public Optional<AuthResult> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> passwordHasher.matches(password, user.getPasswordHash()))
                .map(user -> {
                    String token = jwtService.generateToken(String.valueOf(user.getId()));
                    tokenRepository.storeToken(token, user.getId());
                    return new AuthResult(user, token);
                });
    }

    public Optional<Long> validateToken(String token) {
        return tokenRepository.findUserIdByToken(token);
    }

    public User getUserById(long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public static class AuthResult {
        private final User user;
        private final String token;

        public AuthResult(User user, String token) {
            this.user = user;
            this.token = token;
        }

        public User getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }
    }

    public static class DuplicateUserException extends RuntimeException {
        public DuplicateUserException(String message) { super(message); }
    }
}