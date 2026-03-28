package com.oramadan.rotto.deathroll;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class DeathrollGame {

    private final long threadId;
    private final long challengerId;
    private final long challengedId;
    private long currentTurnUserId;
    private int currentMaximum;

    @Setter private long activePromptMessageId;

    public DeathrollGame(long threadId, long challengerId, long challengedId, int currentMaximum, long currentTurnUserId) {
        this.threadId = threadId;
        this.challengerId = challengerId;
        this.challengedId = challengedId;
        this.currentMaximum = currentMaximum;
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
