package com.dental.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Application configuration (Singleton).
 *
 * <p>Reads {@code config.properties} from the working directory; if the file is
 * missing the built-in defaults (WAMP defaults) are used so the system still
 * starts.</p>
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties props = new Properties();

    private AppConfig() {
        loadDefaults();
        loadFromDisk();
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    private void loadDefaults() {
        props.setProperty("server.port", "8080");
        props.setProperty("web.root", "../frontend");
        props.setProperty("db.host", "localhost");
        props.setProperty("db.port", "3306");
        props.setProperty("db.name", "dental_db");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", "");
        props.setProperty("db.autoInit", "true");
        props.setProperty("clinic.name", "Sunrise Dental Clinic");
        props.setProperty("clinic.address", "No. 45, Hospital Road, Colombo 05");
        props.setProperty("clinic.phone", "011-2 555 777");
        props.setProperty("clinic.taxRate", "0.00");
        props.setProperty("mail.enabled", "false");
        props.setProperty("mail.from", "noreply@brightsmile.lk");
    }

    private void loadFromDisk() {
        Path file = Path.of("config.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
                System.out.println("[config] loaded " + file.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[config] could not read config.properties: " + e.getMessage());
            }
        } else {
            System.out.println("[config] config.properties not found - using defaults");
        }
    }

    public String get(String key) {
        return props.getProperty(key, "");
    }

    public String get(String key, String fallback) {
        String v = props.getProperty(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(get(key).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool(String key, boolean fallback) {
        String v = get(key).trim();
        return v.isEmpty() ? fallback : Boolean.parseBoolean(v);
    }

    /** JDBC URL without the schema name - used to create the database itself. */
    public String serverUrl() {
        return "jdbc:mysql://" + get("db.host") + ":" + get("db.port")
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /** JDBC URL pointing at the application schema. */
    public String jdbcUrl() {
        return "jdbc:mysql://" + get("db.host") + ":" + get("db.port") + "/" + get("db.name")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    }
}
