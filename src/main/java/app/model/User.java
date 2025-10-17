package app.model;

import java.time.Instant;
import java.util.Objects;

public class User {
    private final String id;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final Instant createdAt;

    public User(String id, String username, String passwordHash, String displayName, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.displayName = displayName;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}
