package app.repo.sql;

import app.model.RatingLike;
import app.repo.RatingLikeRepository;
import app.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlRatingLikeRepository implements RatingLikeRepository {

    private final Database database;

    public SqlRatingLikeRepository(Database database) {
        this.database = database;
    }

    @Override
    public RatingLike save(RatingLike ratingLike) {
        String sql = "INSERT INTO rating_likes(rating_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, ratingLike.getRatingId());
            ps.setLong(2, ratingLike.getUserId());
            ps.executeUpdate();

            long id = ratingLike.getId();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }

            return new RatingLike(id, ratingLike.getRatingId(), ratingLike.getUserId());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RatingLike> findByUserAndRating(long userId, long ratingId) {
        String sql = "SELECT * FROM rating_likes WHERE user_id = ? AND rating_id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, ratingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<RatingLike> findByRating(long ratingId) {
        String sql = "SELECT * FROM rating_likes WHERE rating_id = ?";

        List<RatingLike> list = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, ratingId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RatingLike mapRow(ResultSet rs) throws SQLException {
        return new RatingLike(
                rs.getLong("id"),
                rs.getLong("rating_id"),
                rs.getLong("user_id")
        );
    }
}