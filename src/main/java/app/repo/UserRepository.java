package app.repo;

import app.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(long id);
    List<User> findAll();
    User save(User user);
    User update(User user);
}