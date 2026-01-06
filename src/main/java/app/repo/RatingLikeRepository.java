package app.repo;

import app.model.RatingLike;

import java.util.List;
import java.util.Optional;

public interface RatingLikeRepository {
    RatingLike save(RatingLike like);
    Optional<RatingLike> findByUserAndRating(long userId, long ratingId);
    List<RatingLike> findByRating(long ratingId);
}