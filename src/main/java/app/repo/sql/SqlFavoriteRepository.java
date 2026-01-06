package app.repo.sql;

import app.model.Favorite;
import app.repo.FavoriteRepository;
import app.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlFavoriteRepository implements FavoriteRepository {
    private final Database database;

    public SqlFavoriteRepository(Database database) { this.database = database; }

    @Override
    public Favorite save(Favorite favorite) {
        String sql = "INSERT INTO favorites(media_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, favorite.getMediaId());
            ps.setLong(2, favorite.getUserId());
            ps.executeUpdate();
            long id = favorite.getId();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) id = keys.getLong(1);
            }
            return new Favorite(id, favorite.getUserId(), favorite.getMediaId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(long userId, long mediaId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND media_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, mediaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Favorite> findByUserAndMedia(long userId, long mediaId) {
        String sql = "SELECT * FROM favorites WHERE user_id = ? AND media_id = ?";
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
    public List<Favorite> findByUserId(long userId) {
        String sql = "SELECT * FROM favorites WHERE user_id = ?";
        List<Favorite> list = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Favorite mapRow(ResultSet rs) throws SQLException {
        return new Favorite(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("media_id"));
    }
}