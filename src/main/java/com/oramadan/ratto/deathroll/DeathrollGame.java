package com.oramadan.ratto.deathroll;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class DeathrollGame {

    private final long guildId;
    private final long threadId;
    private final long challengerId;
    private final long challengedId;
    private final int startingMaximum;
    private final int wagerChedda;
    private long currentTurnUserId;
    private int currentMaximum;

    @Setter private long activePromptMessageId;

    public DeathrollGame(long guildId, long threadId, long challengerId, long challengedId, int startingMaximum, int wagerChedda, long currentTurnUserId) {
        this.guildId = guildId;
        this.threadId = threadId;
        this.challengerId = challengerId;
        this.challengedId = challengedId;
        this.startingMaximum = startingMaximum;
        this.wagerChedda = wagerChedda;
        this.currentMaximum = startingMaximum;
        this.currentTurnUserId = currentTurnUserId;
    }

    public boolean involves(long userId) {
        return challengerId == userId || challengedId == userId;
    }

    public long otherPlayer(long userId) {
        if (challengerId == userId) {
            return challengedId;
        }
        if (challengedId == userId) {
            return challengerId;
        }
        throw new IllegalArgumentException("User is not in this deathroll game");
    }

    public void advanceTurn(int newMaximum) {
        currentMaximum = newMaximum;
        currentTurnUserId = otherPlayer(currentTurnUserId);
    }
}
