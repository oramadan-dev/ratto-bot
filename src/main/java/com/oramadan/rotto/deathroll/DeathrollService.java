package com.oramadan.rotto.deathroll;

import com.oramadan.rotto.deathroll.dto.DeathrollChallenge;
import com.oramadan.rotto.deathroll.dto.DeathrollRollResult;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeathrollService {

    private static final int STARTING_MAXIMUM = 100;

    private final SecureRandom random = new SecureRandom();
    private final Map<Long, DeathrollChallenge> pendingChallengesByUser = new ConcurrentHashMap<>();
    private final Map<Long, DeathrollGame> activeGamesByThread = new ConcurrentHashMap<>();

    // -------- Challenge Management --------

    public Optional<DeathrollChallenge> createChallenge(long guildId, long channelId, long messageId, long challengerId, long challengedId) {
        // Do not allow a user to be in multiple challenges or games
        if (hasPendingChallenge(challengerId) || hasPendingChallenge(challengedId) || hasActiveGame(challengerId) || hasActiveGame(challengedId)) {
            return Optional.empty();
        }

        DeathrollChallenge challenge = new DeathrollChallenge(guildId, channelId, messageId, challengerId, challengedId);
        pendingChallengesByUser.put(challengedId, challenge);
        return Optional.of(challenge);
    }

    public Optional<DeathrollChallenge> removeChallenge(long guildId, long channelId, long challengedUserId) {
        DeathrollChallenge challenge = pendingChallengesByUser.get(challengedUserId);
        if (challenge == null) {
            return Optional.empty();
        }

        if (challenge.guildId() != guildId || challenge.channelId() != channelId) {
            return Optional.empty();
        }

        pendingChallengesByUser.remove(challengedUserId);
        return Optional.of(challenge);
    }

    // -------- Game Management --------

    public DeathrollGame startGame(long threadId, DeathrollChallenge challenge) {
        DeathrollGame game = new DeathrollGame(threadId, challenge.challengerId(), challenge.challengedId(), STARTING_MAXIMUM, challenge.challengerId());
        activeGamesByThread.put(threadId, game);
        return game;
    }

    public Optional<DeathrollGame> findGame(long threadId) {
        return Optional.ofNullable(activeGamesByThread.get(threadId));
    }

    public Optional<DeathrollGame> removeGame(long threadId) {
        return Optional.ofNullable(activeGamesByThread.remove(threadId));
    }

    public Optional<DeathrollRollResult> roll(long threadId, long userId, long promptMessageId) {
        DeathrollGame game = activeGamesByThread.get(threadId);
        if (game == null || game.getCurrentTurnUserId() != userId || game.getActivePromptMessageId() != promptMessageId) {
            return Optional.empty();
        }

        int previousMaximum = game.getCurrentMaximum();
        int rolledValue = random.nextInt(previousMaximum) + 1;

        if (rolledValue == 1) {
            activeGamesByThread.remove(threadId);
            return Optional.of(new DeathrollRollResult(rolledValue, previousMaximum, true, userId, 0L, 1));
        }

        game.advanceTurn(rolledValue);
        return Optional.of(new DeathrollRollResult(
                rolledValue,
                previousMaximum,
                false,
                0L,
                game.getCurrentTurnUserId(),
                game.getCurrentMaximum()
        ));
    }
    // -------- Helpers --------

    private boolean hasPendingChallenge(long userId) {
        if (pendingChallengesByUser.containsKey(userId)) {
            return true;
        }

        return pendingChallengesByUser.values()
                .stream()
                .anyMatch(challenge -> challenge.challengerId() == userId);
    }

    private boolean hasActiveGame(long userId) {
        return activeGamesByThread.values().stream().anyMatch(game -> game.involves(userId));
    }
}
