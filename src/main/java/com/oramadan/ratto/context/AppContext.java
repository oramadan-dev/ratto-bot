package com.oramadan.ratto.context;

import com.oramadan.ratto.auth.AuthorizationService;
import com.oramadan.ratto.config.AppConfig;
import com.oramadan.ratto.config.BotConfig;
import com.oramadan.ratto.config.CurrencyConfig;
import com.oramadan.ratto.currency.CurrencyRepository;
import com.oramadan.ratto.currency.CurrencyService;
import com.oramadan.ratto.ttrpg.TtrpgRepository;
import com.oramadan.ratto.ttrpg.TtrpgService;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppContext {

    private final AppConfig appConfig;
    private final BotConfig botConfig;
    private final AuthorizationService authorizationService;

    private final CurrencyRepository currencyRepository;
    private final CurrencyService currencyService;
    private final TtrpgRepository ttrpgRepository;
    private final TtrpgService ttrpgService;

    public AppContext(
            AppConfig appConfig,
            BotConfig botConfig,
            CurrencyConfig currencyConfig
    ) {
        this.appConfig = appConfig;
        this.botConfig = botConfig;
        this.authorizationService = new AuthorizationService(appConfig.getSuperadminUserIds());

        this.currencyRepository = new CurrencyRepository(currencyConfig);
        this.currencyService = new CurrencyService(currencyRepository);

        this.ttrpgRepository = new TtrpgRepository();
        this.ttrpgService = new TtrpgService(ttrpgRepository, authorizationService);
    }

}
