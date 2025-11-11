package com.example.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Minimal example with sane defaults and safe patterns. */
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
                while (rs.next()) {                 // simple tight loop (efficient)
                    result.add(rs.getString(1));
                }
                // Return empty list if none; never null
                return result;
            }
        } catch (SQLException e) {
            // Mask PII: log only a hash/length, never raw user input or secrets
            log.error("DB error while fetching user by name (masked) nameHash={}, len={}",
                      safeName.hashCode(), safeName.length(), e);
            return Collections.emptyList();
        }
    }

    /** Small demo runner (no hardcoded secrets; uses primitives; no sleeps). */
    public static void main(String[] args) {
        long startNs = System.nanoTime(); // primitive long (no boxing)

        String user = (args.length > 0) ? args[0] : "admin";
        if ("admin".equals(user)) { // correct String comparison
            log.info("Admin path at {}", TS_FMT.format(ZonedDateTime.now()));
        }

        // Example: read secrets from env (do not log them)
        String apiKey = System.getenv("THIRDPARTY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Third-party API key not configured");
        }

        // Tiny in-memory H2 provider so the example runs as-is
        ConnectionProvider cp = new H2MemoryConnectionProvider();

        List<String> users = findUsers(cp, user);

        // Light stream use (distinct/sorted/join) — not overused
        String summary = users.stream()
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("<none>");
        log.info("Users: {}", summary);

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L; // primitive math
        log.info("Completed in {} ms", elapsedMs);
    }

    /** Abstraction to keep DB handling testable and DI-friendly. */
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    /** Minimal, safe H2 setup for demo/testing (no secrets, no logs of PII). */
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
                throw new IllegalStateException("Failed to init in-mem DB", e);
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL);
        }
    }
}
