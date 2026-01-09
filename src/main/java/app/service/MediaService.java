package app.service;

import app.model.MediaEntry;
import app.model.enums.AgeRestriction;
import app.model.enums.MediaType;
import app.repo.MediaRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class MediaService {
    private final MediaRepository mediaRepository;

    public MediaService(MediaRepository mediaRepository) { this.mediaRepository = mediaRepository; }

    public Optional<MediaEntry> create(String title, String description, String type, Integer releaseYear,
                                       AgeRestriction ageRestriction, List<String> genres, long userId) {
        if (title == null || title.isBlank() || type == null) return Optional.empty();
        MediaType mediaType;
        try{
            mediaType = MediaType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        List<String> safeGenres = genres == null ? List.of() : genres;
        MediaEntry mediaEntry = new MediaEntry(0L, title.trim(), description, mediaType, releaseYear,
                ageRestriction, safeGenres, userId, Instant.now(), 0.0, 0);
        return Optional.of(mediaRepository.save(mediaEntry));
    }

    public Optional<MediaEntry> update(long mediaId, long requesterId, String title, String description, Integer releaseYear,
                                       AgeRestriction ageRestriction, List<String> genres) {
        Optional<MediaEntry> found = mediaRepository.findById(mediaId);
        if (found.isEmpty()) return Optional.empty();
        MediaEntry existing = found.get();
        if (existing.getCreatedByUserId() != requesterId) {
            throw new UnauthorizedException("User cannot modify this media");
        }
        MediaEntry updated = new MediaEntry(existing.getId(), title == null ? existing.getTitle() : title.trim(),
                description == null ? existing.getDescription() : description,
                existing.getType(), releaseYear == null ? existing.getReleaseYear() : releaseYear,
                ageRestriction == null ? existing.getAgeRestriction() : ageRestriction,
                genres == null || genres.isEmpty() ? existing.getGenres() : genres,
                existing.getCreatedByUserId(), existing.getCreatedAt(), existing.getAverageRating(), existing.getRatingCount());
        mediaRepository.update(updated);
        return Optional.of(updated);
    }

    public boolean delete(long mediaId, long requesterId) {
        Optional<MediaEntry> found = mediaRepository.findById(mediaId);
        if (found.isEmpty()) return false;
        if (found.get().getCreatedByUserId() != requesterId) {
            throw new UnauthorizedException("User cannot delete this media");
        }
        return mediaRepository.delete(mediaId);
    }

    public List<MediaEntry> findAll(MediaQuery query) {
        return mediaRepository.findAll().stream()
                .filter(entry -> query.title == null || entry.getTitle().toLowerCase(Locale.ROOT).contains(query.title.toLowerCase(Locale.ROOT)))
                .filter(entry -> query.genre == null || entry.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(query.genre)))
                .filter(entry -> query.mediaType == null || entry.getType().name().equalsIgnoreCase(query.mediaType))
                .filter(entry -> query.releaseYear == null || query.releaseYear.equals(entry.getReleaseYear()))
                .filter(entry -> query.ageRestriction == null || query.ageRestriction == entry.getAgeRestriction())
                .filter(entry -> query.minRating == null || entry.getAverageRating() >= query.minRating)
                .sorted(query.buildComparator())
                .collect(Collectors.toList());
    }

    public Optional<MediaEntry> findById(long mediaId) {
        return mediaRepository.findById(mediaId);
    }

    public MediaEntry updateAggregates(long mediaId, double average, int count) {
        MediaEntry entry = mediaRepository.findById(mediaId).orElseThrow();
        MediaEntry withAgg = entry.withAggregates(average, count);
        mediaRepository.update(withAgg);
        return withAgg;
    }

    public record MediaQuery(String title, String genre, String mediaType, Integer releaseYear,
                             AgeRestriction ageRestriction, Double minRating, String sortBy) {
        public Comparator<MediaEntry> buildComparator() {
            if ("year".equalsIgnoreCase(sortBy)) {
                return Comparator.comparing(entry -> Optional.ofNullable(entry.getReleaseYear()).orElse(0));
            }
            if ("score".equalsIgnoreCase(sortBy)) {
                return Comparator.comparing(MediaEntry::getAverageRating).reversed();
            }
            return Comparator.comparing(MediaEntry::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}