package app.service;

import app.model.Rating;
import app.model.RatingLike;
import app.repo.RatingLikeRepository;
import app.repo.RatingRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class RatingService {
    private final RatingRepository ratingRepository;
    private final RatingLikeRepository likeRepository;
    private final MediaService mediaService;

    public RatingService(RatingRepository ratingRepository, RatingLikeRepository likeRepository, MediaService mediaService) {
        this.ratingRepository = ratingRepository;
        this.likeRepository = likeRepository;
        this.mediaService = mediaService;
    }

    public Rating upsertRating(long mediaId, long userId, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }
        Optional<Rating> existing = ratingRepository.findByUserIdAndMediaId(userId, mediaId);
        Rating stored;
        if (existing.isPresent()) {
            Rating updated = existing.get().update(stars, comment, false);
            stored = ratingRepository.update(updated);
        } else {
            Instant now = Instant.now();
            Rating rating = new Rating(0L, mediaId, userId, stars, comment, false, now, now, 1);
            stored = ratingRepository.save(rating);
        }
        recomputeMedia(mediaId);
        return stored;
    }

    public Optional<Rating> updateRating(long ratingId, long userId, int stars, String comment) {
        Optional<Rating> found = ratingRepository.findById(ratingId);
        if (found.isEmpty()) return Optional.empty();
        if (found.get().getUserId() != userId) {
            throw new UnauthorizedException("Cannot edit another user's rating");
        }
        Rating updated = found.get().update(stars, comment, false);
        Rating stored = ratingRepository.update(updated);
        recomputeMedia(found.get().getMediaId());
        return Optional.of(stored);
    }

    public boolean delete(long ratingId, long userId) {
        Optional<Rating> found = ratingRepository.findById(ratingId);
        if (found.isEmpty()) return false;
        if (found.get().getUserId() != userId) {
            throw new UnauthorizedException("Cannot delete another user's rating");
        }
        boolean removed = ratingRepository.delete(ratingId);
        recomputeMedia(found.get().getMediaId());
        return removed;
    }

    public Optional<Rating> confirm(long ratingId, long userId) {
        Optional<Rating> found = ratingRepository.findById(ratingId);
        if (found.isEmpty()) return Optional.empty();
        if (found.get().getUserId() != userId) {
            throw new UnauthorizedException("Cannot confirm another user's rating");
        }
        Rating updated = found.get().update(found.get().getStars(), found.get().getComment(), true);
        return Optional.of(ratingRepository.update(updated));
    }

    public Optional<RatingLike> like(long ratingId, long userId) {
        Optional<Rating> rating = ratingRepository.findById(ratingId);
        if (rating.isEmpty() || rating.get().getUserId() == userId) {
            return Optional.empty();
        }
        if (likeRepository.findByUserAndRating(userId, ratingId).isPresent()) {
            return Optional.empty();
        }
        RatingLike like = new RatingLike(0L, ratingId, userId);
        return Optional.of(likeRepository.save(like));
    }

    public List<Rating> findByMediaId(long mediaId) {
        return ratingRepository.findByMediaId(mediaId);
    }

    public List<Rating> findByUserId(long userId) { return ratingRepository.findByUserId(userId); }

    private void recomputeMedia(long mediaId) {
        List<Rating> ratings = ratingRepository.findByMediaId(mediaId);
        double avg = ratings.stream().mapToInt(Rating::getStars).average().orElse(0.0);
        mediaService.updateAggregates(mediaId, avg, ratings.size());
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}