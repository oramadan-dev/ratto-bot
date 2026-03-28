package com.oramadan.ratto.deathroll.dto;

public record DeathrollChallenge(
        long guildId,
        long channelId,
        long messageId,
        long challengerId,
        long challengedId
) {
}
