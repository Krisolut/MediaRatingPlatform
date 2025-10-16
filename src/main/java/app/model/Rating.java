package app.model;

import java.time.Instant;
import java.util.Objects;

public class Rating {
    private final String id;
    private final String mediaId;
    private final String userId;
    private final int stars;
    private final String comment;
    private final Instant createdAt;

    public Rating(String id, String mediaId, String userId, Integer stars, String comment, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.mediaId = Objects.requireNonNull(mediaId);
        this.userId = Objects.requireNonNull(userId);
        this.stars = stars;
        this.comment = comment;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() { return id; }
    public String getMediaId() { return mediaId; }
    public String getUserId() { return userId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
