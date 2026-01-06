package app.repo.sql;

import app.model.Rating;
import app.repo.RatingRepository;
import app.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlRatingRepository implements RatingRepository {
    private final Database database;

    public SqlRatingRepository(Database database) {
        this.database = database;
    }

    @Override
    public Rating save(Rating rating) {
        String sql = "INSERT INTO ratings(media_id, user_id, stars, comment, is_confirmed, created_at, updated_at, activity_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getMediaId());
            ps.setLong(2, rating.getUserId());
            ps.setInt(3, rating.getStars());
            ps.setString(4, rating.getComment());
            ps.setBoolean(5, rating.isConfirmed());
            ps.setTimestamp(6, Timestamp.from(rating.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(rating.getUpdatedAt()));
            ps.setInt(8, rating.getActivityCount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Rating(id, rating.getMediaId(), rating.getUserId(), rating.getStars(), rating.getComment(), rating.isConfirmed(), rating.getCreatedAt(), rating.getUpdatedAt(), rating.getActivityCount());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to insert rating");
    }

    @Override
    public Rating update(Rating rating) {
        String sql = "UPDATE ratings SET stars = ?, comment = ?, is_confirmed = ?, updated_at = ?, activity_count = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getStars());
            ps.setString(2, rating.getComment());
            ps.setBoolean(3, rating.isConfirmed());
            ps.setTimestamp(4, Timestamp.from(rating.getUpdatedAt()));
            ps.setInt(5, rating.getActivityCount());
            ps.setLong(6, rating.getId());
            ps.executeUpdate();
            return rating;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(long ratingId) {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ratingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Rating> findByMediaId(long mediaId) {
        return queryList("SELECT * FROM ratings WHERE media_id = ?", mediaId);
    }

    @Override
    public Optional<Rating> findByUserIdAndMediaId(long userId, long mediaId) {
        String sql = "SELECT * FROM ratings WHERE user_id = ? AND media_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Rating> findById(long ratingId) {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ratingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Rating> findByUserId(long userId) {
        return queryList("SELECT * FROM ratings WHERE user_id = ?", userId);
    }

    @Override
    public List<Rating> findAll() {
        return queryList("SELECT * FROM ratings", null);
    }

    private List<Rating> queryList(String sql, Long param) {
        List<Rating> ratings = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ratings.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ratings;
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        return new Rating(
                rs.getLong("id"),
                rs.getLong("media_id"),
                rs.getLong("user_id"),
                rs.getInt("stars"),
                rs.getString("comment"),
                rs.getBoolean("is_confirmed"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getInt("activity_count")
        );
    }
}