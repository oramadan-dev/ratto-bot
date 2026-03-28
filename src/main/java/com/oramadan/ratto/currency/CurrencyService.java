package com.oramadan.ratto.currency;

public class CurrencyService {

    private final CurrencyRepository repository;

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    // -------- Currency Management --------

    public int getCheddaFor(long userId) {
        return repository
                .findByUserId(userId)
                .getChedda();
    }

    public boolean hasChedda(long userId, int chedda) {
        return repository
                .findByUserId(userId)
                .hasChedda(chedda);
    }

    public void addChedda(long userId, int chedda) {
        CurrencyEntity userCurrency = repository.findByUserId(userId);
        userCurrency.addChedda(chedda);
        repository.save(userCurrency);
    }

    public void removeChedda(long userId, int chedda) {
        CurrencyEntity userCurrency = repository.findByUserId(userId);
        userCurrency.removeChedda(chedda);
        repository.save(userCurrency);
    }

}
