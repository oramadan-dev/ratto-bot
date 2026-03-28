package com.oramadan.ratto.bot;

import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.context.AppContext;
import com.oramadan.ratto.currency.CurrencyCommandListener;
import com.oramadan.ratto.deathroll.DeathrollCommandListener;
import com.oramadan.ratto.ttrpg.TtrpgCommandListener;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotFactory {

    public static final Logger logger = LoggerFactory.getLogger(BotFactory.class);

    public static JDA create(@NonNull AppContext appContext) {
        logger.info("Bootstrapping ratto bot");

        BotConfig botConfig = appContext.getBotConfig();
        JDABuilder builder = JDABuilder.createDefault(botConfig.getToken());

        String activityType = botConfig.getActivityType();
        String activityText = botConfig.getActivityText();

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
        builder.addEventListeners(new DeathrollCommandListener(appContext.getCurrencyService()));
        builder.addEventListeners(new CurrencyCommandListener(appContext.getCurrencyService()));
        builder.addEventListeners(new TtrpgCommandListener(appContext.getTtrpgService()));

        JDA bot = builder.build();
        BotCommandRegistrar.register(bot);
        logger.info("Bot successfully initialized");

        return bot;
    }
}
