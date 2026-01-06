package app.repo.sql;

import app.model.User;
import app.repo.UserRepository;
import app.util.Database;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlUserRepository implements UserRepository {
    private final Database database;

    public SqlUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users(username, password_hash, email, favorite_genre, total_ratings, average_given_rating, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFavoriteGenre());
            ps.setInt(5, user.getTotalRatings());
            ps.setDouble(6, user.getAverageGivenRating());
            ps.setTimestamp(7, Timestamp.from(user.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new User(id, user.getUsername(), user.getPasswordHash(), user.getEmail(),
                            user.getFavoriteGenre(), user.getTotalRatings(), user.getAverageGivenRating(), user.getCreatedAt());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to insert user");
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, favorite_genre = ?, total_ratings = ?, average_given_rating = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFavoriteGenre());
            ps.setInt(3, user.getTotalRatings());
            ps.setDouble(4, user.getAverageGivenRating());
            ps.setLong(5, user.getId());
            ps.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("email"),
                rs.getString("favorite_genre"),
                rs.getInt("total_ratings"),
                rs.getDouble("average_given_rating"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}