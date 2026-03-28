package com.oramadan.ratto.deathroll.dto;

public record DeathrollPair(
        long lowerUserId,
        long higherUserId
) {

    public static DeathrollPair of(long firstUserId, long secondUserId) {
        if (firstUserId < secondUserId) {
            return new DeathrollPair(firstUserId, secondUserId);
        }

        return new DeathrollPair(secondUserId, firstUserId);
    }
}
