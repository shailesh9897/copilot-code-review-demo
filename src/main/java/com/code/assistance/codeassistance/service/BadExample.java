package com.code.assistance.codeassistance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Minimal example with safe patterns + closeable DB provider. */
public final class BadExample {

    private static final Logger log = LoggerFactory.getLogger(BadExample.class);

    // Thread-safe formatter (replaces SimpleDateFormat)
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private BadExample() {}

    /** Null-safe, SQL-safe, resource-safe lookup. Never returns null. */
    static List<String> findUsers(ConnectionProvider cp, String name) {
        final String safeName = Optional.ofNullable(name).map(String::trim).orElse("");
        if (safeName.isEmpty()) {
            log.info("findUsers called with empty name at {}", TS_FMT.format(ZonedDateTime.now()));
            return Collections.emptyList();
        }

        final String sql = "SELECT name FROM users WHERE name = ?";

        try (Connection conn = cp.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, safeName); // parameterized; prevents SQL injection

            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) { // efficient tight loop
                    result.add(rs.getString(1));
                }
                return result; // empty if none; never null
            }
        } catch (SQLException e) {
            // Mask PII: only hash/length
            log.error("DB error while fetching user by name (masked) nameHash={}, len={}",
                      safeName.hashCode(), safeName.length(), e);
            return Collections.emptyList();
        }
    }

    /** Demo runner: uses primitives, no sleeps, no secrets logged. */
    public static void main(String[] args) {
        long startNs = System.nanoTime(); // primitive (no boxing)

        String user = (args.length > 0) ? args[0] : "admin";
        if ("admin".equals(user)) { // correct comparison
            log.info("Admin path at {}", TS_FMT.format(ZonedDateTime.now()));
        }

        String apiKey = System.getenv("THIRDPARTY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Third-party API key not configured");
        }

        // Closeable provider (lifecycle managed)
        try (ConnectionProvider cp = new H2MemoryConnectionProvider()) {
            List<String> users = findUsers(cp, user);

            String summary = users.stream()
                    .map(String::toUpperCase)
                    .distinct()
                    .sorted()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("<none>");
            log.info("Users: {}", summary);
        } catch (SQLException e) {
            log.error("Fatal DB setup/close error", e);
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("Completed in {} ms", elapsedMs);
    }

    /** DI-friendly and closeable connection provider. */
    public interface ConnectionProvider extends AutoCloseable {
        Connection getConnection() throws SQLException;
        @Override
        void close() throws SQLException; // explicit lifecycle hook
    }

    /** Safe in-memory H2 with proper shutdown on close (no secrets). */
    static final class H2MemoryConnectionProvider implements ConnectionProvider {
        private static final String URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

        H2MemoryConnectionProvider() {
            try (Connection c = DriverManager.getConnection(URL);
                 Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS users(
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100)
                    )
                """);
                st.execute("INSERT INTO users(name) VALUES('admin'),('alice'),('bob')");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to init in-mem Database", e);
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL);
        }

        @Override
        public void close() throws SQLException {
            // Cleanly shut down the in-memory database
            try (Connection c = DriverManager.getConnection(URL);
                 Statement st = c.createStatement()) {
                st.execute("SHUTDOWN");
            }
        }
    }

    /** Optional custom unchecked exception if you prefer bubbling up errors. */
    public static final class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) { super(message, cause); }
    }
}
