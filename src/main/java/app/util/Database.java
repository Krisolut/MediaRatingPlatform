package app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String db = System.getenv().getOrDefault("DB_NAME", "mediaratingplatform");
        this.user = System.getenv().getOrDefault("DB_USER", "mrpadmin");
        this.password = System.getenv().getOrDefault("DB_PASS",
                System.getenv().getOrDefault("DB_PASSWORD", "mrppass"));
        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
    }

    public Connection getConnection() throws SQLException {
        int attempts = 0;
        long waitMs = 300;

        while (true) {
            try {
                return DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

                boolean dbStarting =
                        msg.contains("starting up") ||
                                msg.contains("the database system is starting");

                attempts++;

                if (!dbStarting || attempts >= 15) {
                    throw ex; // echter Fehler oder zu viele Versuche
                }

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }

                waitMs = Math.min(2000, (long) (waitMs * 1.5));
            }
        }
    }



    public void initializeSchema(Path schemaPath) throws IOException, SQLException {
        String sql = Files.readString(schemaPath);
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(false);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            connection.commit();
        }
    }
}