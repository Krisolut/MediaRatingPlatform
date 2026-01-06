package app.repo;

import java.util.Optional;

public interface TokenRepository {
    void storeToken(String token, long userId);
    Optional<Long> findUserIdByToken(String token);
    void revoke(String token);
}