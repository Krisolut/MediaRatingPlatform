package app.service;

import app.model.User;
import app.repo.RatingRepository;
import app.repo.UserRepository;

import java.util.Optional;

public class UserService {
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    public UserService(UserRepository userRepository, RatingRepository ratingRepository) {
        this.userRepository = userRepository;
        this.ratingRepository = ratingRepository;
    }

    public Optional<User> findById(long id) { return userRepository.findById(id); }

    public Optional<User> updateProfile(long userId, String email, String favoriteGenre) {
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) return Optional.empty();
        User updated = found.get().withProfile(email, favoriteGenre);
        return Optional.of(userRepository.update(updated));
    }

    public User updateStatistics(long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        var ratings = ratingRepository.findByUserId(userId);

        var latestByMedia = ratings.stream()
                .collect(java.util.stream.Collectors.toMap(
                        app.model.Rating::getMediaId,
                        r -> r,
                        (a, b) -> a.getUpdatedAt().isAfter(b.getUpdatedAt()) ? a : b
                ));

        double avg = latestByMedia.values().stream()
                .mapToInt(app.model.Rating::getStars)
                .average()
                .orElse(0.0);

        User withStats = user.withStatistics(latestByMedia.size(), avg);
        return userRepository.update(withStats);
    }
}