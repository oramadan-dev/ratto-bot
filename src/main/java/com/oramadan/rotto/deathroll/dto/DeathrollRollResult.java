package com.oramadan.rotto.deathroll.dto;

public record DeathrollRollResult(
        int rolledValue,
        int previousMaximum,
        boolean gameOver,
        long losingUserId,
        long nextTurnUserId,
        int nextMaximum
) {
}
