package com.oramadan.ratto.currency;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyService {

    private static final int MAX_SCAVENGES_PER_HOUR = 3;
    private static final Duration SCAVENGE_WINDOW = Duration.ofHours(1);

    private final CurrencyRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final Map<CurrencyId, Deque<Instant>> scavengeAttemptsByUser = new ConcurrentHashMap<>();

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    // -------- Currency Management --------

    public int getCheddaFor(long guildId, long userId) {
        return repository
                .findByUserId(guildId, userId)
                .getChedda();
    }

    public boolean hasChedda(long guildId, long userId, int chedda) {
        return repository
                .findByUserId(guildId, userId)
                .hasChedda(chedda);
    }

    public void addChedda(long guildId, long userId, int chedda) {
        CurrencyEntity userCurrency = repository.findByUserId(guildId, userId);
        userCurrency.addChedda(chedda);
        repository.save(userCurrency);
    }

    public void removeChedda(long guildId, long userId, int chedda) {
        CurrencyEntity userCurrency = repository.findByUserId(guildId, userId);
        userCurrency.removeChedda(chedda);
        repository.save(userCurrency);
    }

    public List<CurrencyLeaderboardEntry> getLeaderboard(long guildId) {
        return repository.findAllOrderByCheddaDesc(guildId)
                .stream()
                .map(currencyEntity -> new CurrencyLeaderboardEntry(currencyEntity.getUserId(), currencyEntity.getChedda()))
                .toList();
    }

    public CurrencyScavengeResult scavenge(long guildId, long userId) {
        CurrencyId currencyId = new CurrencyId(guildId, userId);
        Deque<Instant> attempts = scavengeAttemptsByUser.computeIfAbsent(currencyId, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();

        synchronized (attempts) {
            clearExpiredAttempts(attempts, now);

            if (attempts.size() >= MAX_SCAVENGES_PER_HOUR) {
                return new CurrencyScavengeResult(true, 0, 0);
            }

            attempts.addLast(now);
            int awardedChedda = rollScavengeReward();
            if (awardedChedda > 0) {
                addChedda(guildId, userId, awardedChedda);
            }

            return new CurrencyScavengeResult(false, awardedChedda, MAX_SCAVENGES_PER_HOUR - attempts.size());
        }
    }

    private void clearExpiredAttempts(Deque<Instant> attempts, Instant now) {
        while (!attempts.isEmpty() && attempts.peekFirst().plus(SCAVENGE_WINDOW).isBefore(now)) {
            attempts.removeFirst();
        }
    }

    private int rollScavengeReward() {
        int roll = random.nextInt(100);

        if (roll < 10) {
            return random.nextInt(3) + 3;
        }

        if (roll < 20) {
            return 2;
        }

        if (roll < 50) {
            return 1;
        }

        return 0;
    }

}
