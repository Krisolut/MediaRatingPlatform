package app.repo;

import app.model.Favorite;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository {
    Favorite save(Favorite favorite);
    boolean delete(long userId, long mediaId);
    Optional<Favorite> findByUserAndMedia(long userId, long mediaId);
    List<Favorite> findByUserId(long userId);
}