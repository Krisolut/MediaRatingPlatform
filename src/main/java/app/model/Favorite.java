package app.model;

public class Favorite {
    private final long id;
    private final long userId;
    private final long mediaId;

    public Favorite(long id, long userId, long mediaId) {
        this.id = id;
        this.userId = userId;
        this.mediaId = mediaId;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public long getMediaId() { return mediaId; }
}