package app.repo;

import app.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);

    User save(User user);
}
