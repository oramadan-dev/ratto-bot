package com.oramadan.rotto;

import com.oramadan.rotto.bot.BotFactory;
import com.oramadan.rotto.config.BotConfig;
import com.oramadan.rotto.config.Config;

public class RottoApplication {

    public static final String CONFIG_PATH = "config/config.properties";

    public static void main(String[] args) {

        Config config = Config.load(CONFIG_PATH);
        BotConfig botConfig = new BotConfig(config);

        BotFactory.create(botConfig);
    }
}
