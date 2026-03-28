package com.oramadan.ratto.config;

import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class AppConfig {

    private final Set<Long> superadminUserIds;

    public AppConfig(@NonNull Config config) {
        String rawSuperadmins = config.optional("app.superadmin-user-ids", "");
        this.superadminUserIds = Arrays.stream(rawSuperadmins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException exception) {
                        throw new IllegalStateException("Invalid superadmin user id: " + value, exception);
                    }
                })
                .collect(Collectors.toUnmodifiableSet());
    }
}
