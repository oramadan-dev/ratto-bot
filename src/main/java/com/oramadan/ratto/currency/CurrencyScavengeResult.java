package com.oramadan.ratto.currency;

public record CurrencyScavengeResult(
        boolean rateLimited,
        int awardedChedda,
        int attemptsRemaining
) {
}
