package com.evacsim.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;

/**
 * SQLite connection and relational schema (operators → scenarios → runs).
 */
public final class Database {

    private static final String URL = "jdbc:sqlite:evacsim.db";
    private static Connection connection;

    private Database() {
    }

    public static synchronized void initialize() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
            }
            createSchema();
            seedOperators();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open SQLite database", e);
        }
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // shutdown
            }
            connection = null;
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS operators (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS scenarios (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        student_count INTEGER NOT NULL,
                        shy_percent REAL NOT NULL,
                        notes TEXT,
                        operator_id INTEGER NOT NULL,
                        FOREIGN KEY (operator_id) REFERENCES operators(id) ON DELETE CASCADE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS simulation_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        scenario_id INTEGER,
                        operator_id INTEGER NOT NULL,
                        generations INTEGER NOT NULL,
                        evacuated INTEGER NOT NULL,
                        collisions INTEGER NOT NULL,
                        weather_summary TEXT,
                        finished INTEGER NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE SET NULL,
                        FOREIGN KEY (operator_id) REFERENCES operators(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    private static void seedOperators() throws SQLException {
        try (var ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO operators(username, password_hash) VALUES (?, ?)")) {
            ps.setString(1, "admin");
            ps.setString(2, hashPassword("admin123"));
            ps.executeUpdate();
            ps.setString(1, "operator");
            ps.setString(2, hashPassword("pass123"));
            ps.executeUpdate();
        }
    }
}
