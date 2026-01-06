package app.service;

import app.model.Favorite;
import app.model.MediaEntry;
import app.repo.FavoriteRepository;
import app.repo.MediaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MediaRepository mediaRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, MediaRepository mediaRepository) {
        this.favoriteRepository = favoriteRepository;
        this.mediaRepository = mediaRepository;
    }

    public Optional<Favorite> markFavorite(long userId, long mediaId) {
        if (favoriteRepository.findByUserAndMedia(userId, mediaId).isPresent()) {
            return Optional.empty();
        }
        Favorite favorite = new Favorite(0L, userId, mediaId);
        return Optional.of(favoriteRepository.save(favorite));
    }

    public boolean removeFavorite(long userId, long mediaId) {
        return favoriteRepository.delete(userId, mediaId);
    }

    public List<MediaEntry> listFavorites(long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(f -> mediaRepository.findById(f.getMediaId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}