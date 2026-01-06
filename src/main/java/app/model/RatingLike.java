package app.model;

public class RatingLike {
    private final long id;
    private final long ratingId;
    private final long userId;

    public RatingLike(long id, long ratingId, long userId) {
        this.id = id;
        this.ratingId = ratingId;
        this.userId = userId;
    }

    public long getId() { return id; }
    public long getRatingId() { return ratingId; }
    public long getUserId() { return userId; }
}