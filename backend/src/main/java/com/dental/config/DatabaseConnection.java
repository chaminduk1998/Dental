package com.dental.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <b>Singleton pattern.</b>
 *
 * <p>Single point of access to the MySQL database for the whole application.
 * The class owns the driver registration and the connection settings; every DAO
 * asks this object for a {@link Connection} and closes it with try-with-resources,
 * so connections are never leaked between requests.</p>
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        AppConfig cfg = AppConfig.get();
        this.url = cfg.jdbcUrl();
        this.user = cfg.get("db.user");
        this.password = cfg.get("db.password");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MySQL JDBC driver not found on the classpath. "
                    + "Make sure lib/mysql-connector-j-*.jar is included.", e);
        }
    }

    /** Thread-safe lazy initialisation (double-checked locking). */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /** A fresh connection to the application schema. Caller must close it. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** A connection to the MySQL server itself (no schema selected). */
    public static Connection serverConnection() throws SQLException {
        AppConfig cfg = AppConfig.get();
        return DriverManager.getConnection(cfg.serverUrl(), cfg.get("db.user"), cfg.get("db.password"));
    }

    /** Quick connectivity check used at start-up. */
    public boolean testConnection() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.err.println("[db] connection test failed: " + e.getMessage());
            return false;
        }
    }
}
