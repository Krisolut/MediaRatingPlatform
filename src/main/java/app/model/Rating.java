package app.model;

import java.time.Instant;
import java.util.Objects;

public class Rating {
    private final long id;
    private final long mediaId;
    private final long userId;
    private final int stars;
    private final String comment;
    private final boolean confirmed;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int activityCount;

    public Rating(long id, long mediaId, long userId, int stars, String comment, boolean confirmed,
                  Instant createdAt, Instant updatedAt, int activityCount) {
        this.id = id;
        this.mediaId = mediaId;
        this.userId = userId;
        this.stars = stars;
        this.comment = comment;
        this.confirmed = confirmed;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.activityCount = activityCount;
    }

    public long getId() { return id; }
    public long getMediaId() { return mediaId; }
    public long getUserId() { return userId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public boolean isConfirmed() { return confirmed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getActivityCount() { return activityCount; }

    public Rating update(int stars, String comment, boolean confirmed) {
        return new Rating(id, mediaId, userId, stars, comment, confirmed, createdAt, Instant.now(), activityCount + 1);
    }
}