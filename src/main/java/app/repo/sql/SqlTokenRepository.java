package app.repo.sql;

import app.repo.TokenRepository;
import app.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SqlTokenRepository implements TokenRepository {
    private final Database database;

    public SqlTokenRepository(Database database) { this.database = database; }

    @Override
    public void storeToken(String token, long userId) {
        String sql = "INSERT INTO tokens(token, user_id) VALUES (?, ?) ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        String sql = "SELECT user_id FROM tokens WHERE token = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getLong("user_id"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public void revoke(String token) {
        String sql = "DELETE FROM tokens WHERE token = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}