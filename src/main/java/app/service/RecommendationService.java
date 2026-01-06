package app.service;

import app.model.MediaEntry;
import app.model.Rating;
import app.repo.MediaRepository;
import app.repo.RatingRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendationService {
    private final MediaRepository mediaRepository;
    private final RatingRepository ratingRepository;

    public RecommendationService(MediaRepository mediaRepository, RatingRepository ratingRepository) {
        this.mediaRepository = mediaRepository;
        this.ratingRepository = ratingRepository;
    }

    public List<MediaEntry> recommendByGenre(long userId) {
        Set<String> preferredGenres = ratingRepository.findByUserId(userId).stream()
                .filter(r -> r.getStars() >= 4)
                .flatMap(r -> mediaRepository.findById(r.getMediaId()).stream())
                .flatMap(media -> media.getGenres().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (preferredGenres.isEmpty()) {
            return List.of();
        }
        return mediaRepository.findAll().stream()
                .filter(m -> m.getGenres().stream().anyMatch(g -> preferredGenres.contains(g.toLowerCase())))
                .sorted(Comparator.comparing(MediaEntry::getAverageRating).reversed())
                .collect(Collectors.toList());
    }

    public List<MediaEntry> recommendByContent(long userId) {
        List<Rating> highRatings = ratingRepository.findByUserId(userId).stream()
                .filter(r -> r.getStars() >= 4)
                .toList();
        if (highRatings.isEmpty()) {
            return List.of();
        }
        Set<String> likedGenres = highRatings.stream()
                .flatMap(r -> mediaRepository.findById(r.getMediaId()).stream())
                .flatMap(m -> m.getGenres().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Map<String, Long> typePreference = highRatings.stream()
                .map(r -> mediaRepository.findById(r.getMediaId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(m -> m.getType().name(), Collectors.counting()));
        return mediaRepository.findAll().stream()
                .filter(m -> likedGenres.isEmpty() || m.getGenres().stream().anyMatch(g -> likedGenres.contains(g.toLowerCase())))
                .sorted((a, b) -> {
                    long typeScoreA = typePreference.getOrDefault(a.getType().name(), 0L);
                    long typeScoreB = typePreference.getOrDefault(b.getType().name(), 0L);
                    int cmp = Long.compare(typeScoreB, typeScoreA);
                    if (cmp != 0) return cmp;
                    return Double.compare(b.getAverageRating(), a.getAverageRating());
                })
                .collect(Collectors.toList());
    }
}