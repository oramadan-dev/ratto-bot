package com.oramadan.ratto.config;

import lombok.NonNull;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {

    private final Properties props;

    private Config(Properties props) {
        this.props = props;
    }

    public static Config load(@NonNull String path) {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config at " + path, e);
        }

        return new Config(props);
    }

    public String required(String key) {
        String value = trim(props.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return value;
    }

    public String optional(String key) {
        return trim(props.getProperty(key));
    }

    public String optional(String key, String defaultValue) {
        String value = trim(props.getProperty(key));
        return value == null ? defaultValue : value;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}