package app.repo.memo;

import app.model.Rating;
import app.repo.RatingRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRatingRepository implements RatingRepository {
    private final Map<String, Rating> ratingsById = new ConcurrentHashMap<>();

    @Override
    public Rating save(Rating rating) {
        ratingsById.put(rating.getId(), rating);
        return rating;
    }

    @Override
    public List<Rating> findByMediaId(String mediaId) {
        List<Rating> result = new ArrayList<>();
        for (Rating rating : ratingsById.values()) {
            if (rating.getMediaId().equals(mediaId)) {
                result.add(rating);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<Rating> findByUserIdAndMediaId(String userId, String mediaId) {
        for (Rating rating : ratingsById.values()) {
            if (rating.getUserId().equals(userId) && rating.getMediaId().equals(mediaId)) {
                return Optional.of(rating);
            }
        }
        return Optional.empty();
    }
}
