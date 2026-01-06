package app.repo;

import app.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository {
    Rating save(Rating rating);
    Rating update(Rating rating);
    boolean delete(long ratingId);

    List<Rating> findByMediaId(long mediaId);
    Optional<Rating> findByUserIdAndMediaId(long userId, long mediaId);
    Optional<Rating> findById(long ratingId);
    List<Rating> findByUserId(long userId);
    List<Rating> findAll();
}