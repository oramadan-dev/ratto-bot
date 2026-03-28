package com.oramadan.ratto.context;

import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.config.CurrencyConfig;
import com.oramadan.ratto.currency.CurrencyRepository;
import com.oramadan.ratto.currency.CurrencyService;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppContext {

    private final BotConfig botConfig;

    private final CurrencyRepository currencyRepository;
    private final CurrencyService currencyService;

    public AppContext(
            BotConfig botConfig,
            CurrencyConfig currencyConfig
    ) {
        this.botConfig = botConfig;

        this.currencyRepository = new CurrencyRepository(currencyConfig);
        this.currencyService = new CurrencyService(currencyRepository);
    }

}
