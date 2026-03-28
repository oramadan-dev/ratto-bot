package com.oramadan.ratto.config;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class BotConfig {

    @NonNull private final String token;
    @Nullable private final String activityType;
    @Nullable private final String activityText;

    public BotConfig(@NonNull Config config) {
        this.token = config.required("discord.token");

        this.activityType = config.optional("discord.initial.activity-type");
        this.activityText = config.optional("discord.initial.activity-text");
    }

}
