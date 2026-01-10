package app.service;

import app.model.User;
import app.repo.RatingRepository;
import app.repo.UserRepository;

import java.util.Optional;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findById(long id) { return userRepository.findById(id); }
    public Optional<User> findByIdWithStats(long id) { return userRepository.findByIdWithStats(id); }

    public Optional<User> updateProfile(long userId, String email, String favoriteGenre) {
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) return Optional.empty();
        User updated = found.get().withProfile(email, favoriteGenre);
        userRepository.update(updated);
        return userRepository.findByIdWithStats(userId);
    }
}