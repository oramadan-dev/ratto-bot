package com.oramadan.ratto;

import com.oramadan.ratto.bot.BotFactory;
import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.config.Config;
import com.oramadan.ratto.config.CurrencyConfig;
import com.oramadan.ratto.context.AppContext;

public class RattoApplication {

    public static final String CONFIG_PATH = "config/config.properties";

    public static void main(String[] args) {

        Config config = Config.load(CONFIG_PATH);

        BotConfig botConfig = new BotConfig(config);
        CurrencyConfig currencyConfig = new CurrencyConfig(config);

        AppContext appContext = new AppContext(
                botConfig,
                currencyConfig
        );

        BotFactory.create(appContext);
    }
}
