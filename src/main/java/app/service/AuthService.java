package app.service;

import app.model.User;
import app.repo.UserRepository;
import app.security.JwtService;
import app.security.PasswordHasher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    public Optional<User> register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        if(userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUserException("Username already exist");
        }

        String id = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(password);
        User user = new User(id, username.trim(), passwordHash, null, Instant.now());
        userRepository.save(user);
        return Optional.of(user);
    }

    public Optional<AuthResult> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> passwordHasher.matches(password, user.getPasswordHash()))
                .map(user -> new AuthResult(user, jwtService.generateToken(user.getId())));
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
