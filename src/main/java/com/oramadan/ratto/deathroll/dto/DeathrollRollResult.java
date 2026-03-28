package com.oramadan.ratto.deathroll.dto;

public record DeathrollRollResult(
        int rolledValue,
        int previousMaximum,
        boolean gameOver,
        long losingUserId,
        long nextTurnUserId,
        int nextMaximum
) {
}
