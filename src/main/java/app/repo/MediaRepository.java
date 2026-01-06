package app.repo;

import app.model.MediaEntry;

import java.util.List;
import java.util.Optional;

public interface MediaRepository {
    MediaEntry save(MediaEntry mediaEntry);
    MediaEntry update(MediaEntry entry);
    boolean delete(long id);

    List<MediaEntry> findAll();
    Optional<MediaEntry> findById(long id);
}