package app.service;

import app.model.MediaEntry;
import app.model.Rating;
import app.repo.RatingRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RatingService {
    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {this.ratingRepository = ratingRepository;}

    public Optional<Rating> create(String mediaId, String userId, Integer stars, String comment, MediaEntry mediaEntry) {
        if (mediaId == null || mediaId.isBlank() || userId == null || userId.isBlank() || stars == null) {
            return Optional.empty();
        }
        if (stars < 1 || stars > 5) {
            return Optional.empty();
        }
        if (mediaEntry == null) {
            return Optional.empty();
        }
        if (ratingRepository.findByUserIdAndMediaId(userId, mediaId).isPresent()) {
            throw new DuplicateRatingException("User has already rated this media.");
        }
        Rating rating = new Rating(UUID.randomUUID().toString(), mediaId, userId, stars, comment, Instant.now());
        ratingRepository.save(rating);
        return Optional.of(rating);
    }

    public List<Rating> findByMediaId(String mediaId) {
        return ratingRepository.findByMediaId(mediaId);
    }

    public static class DuplicateRatingException extends RuntimeException {
        public DuplicateRatingException(String message) {
            super(message);
        }
    }
}
