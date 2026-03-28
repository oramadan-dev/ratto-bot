package com.oramadan.ratto.config;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CurrencyConfig {

    private final int defaultChedda;

    public CurrencyConfig(@NonNull Config config) {
        this.defaultChedda = config.optionalInt("currency.default-chedda", 100);
    }
}
