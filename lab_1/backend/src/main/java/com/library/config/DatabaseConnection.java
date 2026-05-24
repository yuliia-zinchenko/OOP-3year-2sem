package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * GoF: Singleton. Holds a single HikariCP DataSource for the whole app.
 */
public final class DatabaseConnection {
    private static final Logger log = LogManager.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;

    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        Properties p = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (Exception e) {
            log.error("Failed to load db.properties", e);
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(env("DB_URL", p.getProperty("db.url")));
        cfg.setUsername(env("DB_USER", p.getProperty("db.user")));
        cfg.setPassword(env("DB_PASSWORD", p.getProperty("db.password")));
        cfg.setMaximumPoolSize(Integer.parseInt(p.getProperty("db.maxPoolSize", "10")));
        cfg.setDriverClassName("org.postgresql.Driver");

        this.dataSource = new HikariDataSource(cfg);
        log.info("DataSource initialized: {}", cfg.getJdbcUrl());
    }

    public static DatabaseConnection getInstance() {
        DatabaseConnection local = instance;
        if (local == null) {
            synchronized (DatabaseConnection.class) {
                local = instance;
                if (local == null) {
                    instance = local = new DatabaseConnection();
                }
            }
        }
        return local;
    }

    public DataSource getDataSource() { return dataSource; }

    public Connection getConnection() throws SQLException { return dataSource.getConnection(); }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : fallback;
    }
}
