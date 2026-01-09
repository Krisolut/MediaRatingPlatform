package app.repo.sql;

import app.model.MediaEntry;
import app.model.enums.AgeRestriction;
import app.model.enums.MediaType;
import app.repo.MediaRepository;
import app.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SqlMediaRepository implements MediaRepository {
    private final Database database;

    public SqlMediaRepository(Database database) {
        this.database = database;
    }

    @Override
    public MediaEntry save(MediaEntry mediaEntry) {
        String sql = "INSERT INTO media_entries(title, description, media_type, release_year, age_restriction, genres, creator_user_id, average_rating, rating_count, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mediaEntry.getTitle());
            ps.setString(2, mediaEntry.getDescription());
            ps.setString(3, mediaEntry.getType().name());
            if (mediaEntry.getReleaseYear() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, mediaEntry.getReleaseYear());
            if (mediaEntry.getAgeRestriction() == null) ps.setNull(5, Types.VARCHAR); else ps.setString(5, mediaEntry.getAgeRestriction().name());
            ps.setString(6, String.join(",", mediaEntry.getGenres() == null ? List.of() : mediaEntry.getGenres()));
            ps.setLong(7, mediaEntry.getCreatedByUserId());
            ps.setDouble(8, mediaEntry.getAverageRating());
            ps.setInt(9, mediaEntry.getRatingCount());
            ps.setTimestamp(10, Timestamp.from(mediaEntry.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new MediaEntry(id, mediaEntry.getTitle(), mediaEntry.getDescription(), mediaEntry.getType(),
                            mediaEntry.getReleaseYear(), mediaEntry.getAgeRestriction(), mediaEntry.getGenres(),
                            mediaEntry.getCreatedByUserId(), mediaEntry.getCreatedAt(), mediaEntry.getAverageRating(), mediaEntry.getRatingCount());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to insert media");
    }

    @Override
    public MediaEntry update(MediaEntry entry) {
        String sql = "UPDATE media_entries SET title = ?, description = ?, release_year = ?, age_restriction = ?, genres = ?, average_rating = ?, rating_count = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getDescription());
            if (entry.getReleaseYear() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, entry.getReleaseYear());
            if (entry.getAgeRestriction() == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, entry.getAgeRestriction().name());
            ps.setString(5, String.join(",", entry.getGenres() == null ? List.of() : entry.getGenres()));
            ps.setDouble(6, entry.getAverageRating());
            ps.setInt(7, entry.getRatingCount());
            ps.setLong(8, entry.getId());
            ps.executeUpdate();
            return entry;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(long id) {
        String sql = "DELETE FROM media_entries WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<MediaEntry> findAll() {
        String sql = "SELECT * FROM media_entries";
        List<MediaEntry> results = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    @Override
    public Optional<MediaEntry> findById(long id) {
        String sql = "SELECT * FROM media_entries WHERE id = ?";
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

    private MediaEntry mapRow(ResultSet rs) throws SQLException {
        String genreStr = rs.getString("genres");
        List<String> genres = genreStr == null || genreStr.isBlank() ? List.of() : Arrays.stream(genreStr.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        String age = rs.getString("age_restriction");
        return new MediaEntry(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                MediaType.valueOf(rs.getString("media_type")),
                (Integer) rs.getObject("release_year"),
                age == null ? null : AgeRestriction.valueOf(age),
                genres,
                rs.getLong("creator_user_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getDouble("average_rating"),
                rs.getInt("rating_count")
        );
    }
}