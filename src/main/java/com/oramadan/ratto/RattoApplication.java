package com.oramadan.ratto;

import com.oramadan.ratto.bot.BotFactory;
import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.config.Config;

public class RattoApplication {

    public static final String CONFIG_PATH = "config/config.properties";

    public static void main(String[] args) {

        Config config = Config.load(CONFIG_PATH);
        BotConfig botConfig = new BotConfig(config);

        BotFactory.create(botConfig);
    }
}
