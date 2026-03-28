package com.oramadan.ratto.deathroll;

import com.oramadan.ratto.deathroll.dto.DeathrollChallenge;
import com.oramadan.ratto.deathroll.dto.DeathrollPair;
import com.oramadan.ratto.deathroll.dto.DeathrollRollResult;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeathrollService {

    public static final int DEFAULT_STARTING_MAXIMUM = 100;
    private static final int MINIMUM_STARTING_MAXIMUM = 2;
    private static final int MINIMUM_WAGER = 0;

    private final SecureRandom random = new SecureRandom();
    private final Map<DeathrollPair, DeathrollChallenge> pendingChallengesByPair = new ConcurrentHashMap<>();
    private final Map<DeathrollPair, Long> activeThreadIdsByPair = new ConcurrentHashMap<>();
    private final Map<Long, DeathrollGame> activeGamesByThread = new ConcurrentHashMap<>();

    // -------- Challenge Management --------

    public Optional<DeathrollChallenge> createChallenge(
            long guildId,
            long channelId,
            long messageId,
            long challengerId,
            long challengedId,
            int startingMaximum,
            int wagerChedda
    ) {
        if (startingMaximum < MINIMUM_STARTING_MAXIMUM || wagerChedda < MINIMUM_WAGER) {
            return Optional.empty();
        }

        DeathrollPair pair = DeathrollPair.of(challengerId, challengedId);
        if (pendingChallengesByPair.containsKey(pair) || activeThreadIdsByPair.containsKey(pair)) {
            return Optional.empty();
        }

        DeathrollChallenge challenge = new DeathrollChallenge(
                guildId,
                channelId,
                messageId,
                challengerId,
                challengedId,
                startingMaximum,
                wagerChedda
        );
        pendingChallengesByPair.put(pair, challenge);
        return Optional.of(challenge);
    }

    public Optional<DeathrollChallenge> removeChallenge(long guildId, long channelId, long challengerUserId, long challengedUserId) {
        DeathrollPair pair = DeathrollPair.of(challengerUserId, challengedUserId);
        DeathrollChallenge challenge = pendingChallengesByPair.get(pair);
        if (challenge == null) {
            return Optional.empty();
        }

        if (challenge.guildId() != guildId
                || challenge.channelId() != channelId
                || challenge.challengerId() != challengerUserId
                || challenge.challengedId() != challengedUserId) {
            return Optional.empty();
        }

        pendingChallengesByPair.remove(pair);
        return Optional.of(challenge);
    }

    // -------- Game Management --------

    public DeathrollGame startGame(long threadId, DeathrollChallenge challenge) {
        DeathrollGame game = new DeathrollGame(
                challenge.guildId(),
                threadId,
                challenge.challengerId(),
                challenge.challengedId(),
                challenge.startingMaximum(),
                challenge.wagerChedda(),
                challenge.challengerId()
        );
        activeThreadIdsByPair.put(DeathrollPair.of(challenge.challengerId(), challenge.challengedId()), threadId);
        activeGamesByThread.put(threadId, game);
        return game;
    }

    public Optional<DeathrollGame> findGame(long threadId) {
        return Optional.ofNullable(activeGamesByThread.get(threadId));
    }

    public Optional<DeathrollGame> removeGame(long threadId) {
        DeathrollGame game = activeGamesByThread.remove(threadId);
        if (game == null) {
            return Optional.empty();
        }

        activeThreadIdsByPair.remove(DeathrollPair.of(game.getChallengerId(), game.getChallengedId()));
        return Optional.of(game);
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
            activeThreadIdsByPair.remove(DeathrollPair.of(game.getChallengerId(), game.getChallengedId()));
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
}
