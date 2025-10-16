package app.repo;

import app.model.MediaEntry;

import java.util.List;
import java.util.Optional;

public interface MediaRepository {
    MediaEntry save(MediaEntry mediaEntry);

    List<MediaEntry> findAll();
    Optional<MediaEntry> findById(String id);
}
