package com.oramadan.ratto.ttrpg;

import java.time.Instant;
import java.util.List;

public record TtrpgWeekEntry(
        long id,
        String name,
        Instant occurrenceAt,
        boolean recurringWeekly,
        long gmUserId,
        List<Long> playerIds
) {
}
