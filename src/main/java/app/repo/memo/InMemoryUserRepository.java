package app.repo.memo;

import app.model.User;
import app.repo.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        String userId = userIdByUsername.get(username);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public User save(User user) {
        usersById.put(user.getId(), user);
        userIdByUsername.put(user.getUsername(), user.getId());
        return user;
    }
}
