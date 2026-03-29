package com.oramadan.ratto.ttrpg;

import java.time.Instant;
import java.util.List;

public record TtrpgEventDetails(
        long id,
        long guildId,
        long gmUserId,
        String name,
        Instant scheduledAt,
        int recurrenceWeeks,
        List<Long> playerIds
) {
}
