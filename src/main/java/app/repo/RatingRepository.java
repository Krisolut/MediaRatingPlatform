package app.repo;

import app.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository {
    Rating save(Rating rating);

    List<Rating> findByMediaId(String mediaId);
    Optional<Rating> findByUserIdAndMediaId(String userId, String mediaId);
}
