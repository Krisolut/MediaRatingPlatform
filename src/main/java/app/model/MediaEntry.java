package app.model;

import app.model.enums.AgeRestriction;
import app.model.enums.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class MediaEntry {
    private final long id;
    private final String title;
    private final String description;
    private final MediaType type;
    private final Integer releaseYear;
    private final AgeRestriction ageRestriction;
    private final List<String> genres;
    private final long createdByUserId;
    private final Instant createdAt;
    private final double averageRating;
    private final int ratingCount;

    public MediaEntry(long id, String title, String description, MediaType type, Integer releaseYear,
                      AgeRestriction ageRestriction, List<String> genres, long createdByUserId,
                      Instant createdAt, double averageRating, int ratingCount) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.type = Objects.requireNonNull(type);
        this.releaseYear = releaseYear;
        this.ageRestriction = ageRestriction;
        this.genres = genres == null ? List.of() : List.copyOf(genres);
        this.createdByUserId = createdByUserId;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MediaType getType() { return type; }
    public Integer getReleaseYear() { return releaseYear; }
    public AgeRestriction getAgeRestriction() { return ageRestriction; }
    public List<String> getGenres() { return genres; }
    public long getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }

    public MediaEntry withAggregates(double avg, int count) {
        return new MediaEntry(id, title, description, type, releaseYear, ageRestriction, genres, createdByUserId, createdAt, avg, count);
    }
}