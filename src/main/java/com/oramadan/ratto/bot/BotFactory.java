package com.oramadan.ratto.bot;

import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.deathroll.DeathrollCommandListener;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class BotFactory {

    public static JDA create(@NonNull BotConfig config) {

        JDABuilder builder = JDABuilder.createDefault(config.getToken());

        String activityType = config.getActivityType();
        String activityText = config.getActivityText();

        if (activityType != null && activityText != null && !activityText.isBlank()) {
            Activity activity = switch (activityType.toLowerCase()) {
                case "watching" -> Activity.watching(activityText);
                case "playing" -> Activity.playing(activityText);
                default -> null;
            };

            if (activity != null) {
                builder.setActivity(activity);
            }
        }

        // Capabilities
        builder.addEventListeners(new DeathrollCommandListener());

        return builder.build();
    }
}
