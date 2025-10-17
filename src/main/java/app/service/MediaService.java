package app.service;

import app.model.MediaEntry;
import app.repo.MediaRepository;
import app.model.enums.ageRestriction;
import app.model.enums.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MediaService {
    private final MediaRepository mediaRepository;

    public MediaService(MediaRepository mediaRepository) { this.mediaRepository = mediaRepository; }

    public Optional<MediaEntry> create(String title, String type, Integer releaseYear, ageRestriction ageRestriction, String userid) {
        if (title == null || title.isBlank() || type == null || userid == null) return Optional.empty();
        MediaType mediaType;
        try{
            mediaType = MediaType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        MediaEntry mediaEntry = new MediaEntry(UUID.randomUUID().toString(), title.trim(), mediaType, releaseYear, ageRestriction, userid, Instant.now());
        mediaRepository.save(mediaEntry);
        return Optional.of(mediaEntry);
    }

    public List<MediaEntry> findAll() {
        return mediaRepository.findAll();
    }

    public Optional<MediaEntry> findById(String mediaId) {
        return mediaRepository.findById(mediaId);
    }
}
