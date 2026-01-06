package app.model;

import java.time.Instant;
import java.util.Objects;

public class User {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String favoriteGenre;
    private final int totalRatings;
    private final double averageGivenRating;
    private final Instant createdAt;

    public User(long id, String username, String passwordHash, String email, String favoriteGenre,
                int totalRatings, double averageGivenRating, Instant createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.email = email;
        this.favoriteGenre = favoriteGenre;
        this.totalRatings = totalRatings;
        this.averageGivenRating = averageGivenRating;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getFavoriteGenre() { return favoriteGenre; }
    public int getTotalRatings() { return totalRatings; }
    public double getAverageGivenRating() { return averageGivenRating; }
    public Instant getCreatedAt() { return createdAt; }

    public User withStatistics(int totalRatings, double averageGivenRating) {
        return new User(id, username, passwordHash, email, favoriteGenre, totalRatings, averageGivenRating, createdAt);
    }

    public User withProfile(String email, String favoriteGenre) {
        return new User(id, username, passwordHash, email, favoriteGenre, totalRatings, averageGivenRating, createdAt);
    }
}