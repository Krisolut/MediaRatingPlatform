package app.repo;

import app.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(long id);
    Optional<User> findByIdWithStats(long id);
    List<User> findAll();
    List<User> findAllWithStats();
    User save(User user);
    User update(User user);
}