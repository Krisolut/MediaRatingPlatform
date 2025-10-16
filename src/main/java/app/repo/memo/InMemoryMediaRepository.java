package app.repo.memo;

import app.model.MediaEntry;
import app.repo.MediaRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMediaRepository implements MediaRepository {
    private final Map<String, MediaEntry> mediaById = new ConcurrentHashMap<>();

    @Override
    public MediaEntry save(MediaEntry mediaEntry) {
        mediaById.put(mediaEntry.getId(), mediaEntry);
        return mediaEntry;
    }

    @Override
    public List<MediaEntry> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(mediaById.values()));
    }

    @Override
    public Optional<MediaEntry> findById(String id) {
        return Optional.ofNullable(mediaById.get(id));
    }
}
