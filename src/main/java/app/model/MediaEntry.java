package app.model;

import app.model.enums.MediaType;
import app.model.enums.ageRestriction;

import java.time.Instant;
import java.util.Objects;

public class MediaEntry {
    private final String id;
    private final String title;
    private final MediaType type;
    private final Integer releaseYear;
    private final ageRestriction fsk;
    private final String createdByUserId;
    private final Instant createdAt;

    public MediaEntry(String id, String title, MediaType type, Integer releaseYear, ageRestriction fsk, String createdByUserId, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.type = Objects.requireNonNull(type);
        this.releaseYear = releaseYear;
        this.fsk = fsk;
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public MediaType getType() {
        return type;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public ageRestriction getFsk() {
        return fsk;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
