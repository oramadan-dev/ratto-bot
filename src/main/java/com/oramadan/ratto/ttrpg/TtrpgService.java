package com.oramadan.ratto.ttrpg;

import com.oramadan.ratto.auth.AuthorizationService;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TtrpgService {

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String INPUT_TIME_ZONE_LABEL = "GMT+0";
    public static final String RECURRENCE_NONE = "none";
    public static final String RECURRENCE_WEEKLY = "weekly";
    public static final String RECURRENCE_BIWEEKLY = "biweekly";

    private static final Pattern USER_ID_PATTERN = Pattern.compile("\\d+");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    private static final ZoneId INPUT_ZONE_ID = ZoneOffset.UTC;

    private final TtrpgRepository repository;
    private final AuthorizationService authorizationService;

    public TtrpgService(TtrpgRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public TtrpgEventDetails createEvent(long guildId, long gmUserId, String name, String scheduledAtRaw, String recurrenceRaw, String playersRaw) {
        Instant scheduledAt = parseScheduledAt(scheduledAtRaw);
        int recurrenceWeeks = parseRecurrenceWeeks(recurrenceRaw);
        Set<Long> playerIds = parsePlayerIds(playersRaw);
        playerIds.add(gmUserId);

        if (name.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be blank.");
        }

        return repository.saveEvent(
                guildId,
                gmUserId,
                name.trim(),
                scheduledAt,
                recurrenceWeeks,
                new ArrayList<>(playerIds)
        );
    }

    public Optional<TtrpgEventDetails> editEvent(
            long guildId,
            long eventId,
            long actorUserId,
            Long newGmUserId,
            String newName,
            String newScheduledAtRaw,
            String newRecurrenceRaw,
            String playersRaw
    ) {
        if (newName == null && newScheduledAtRaw == null && newRecurrenceRaw == null && newGmUserId == null && playersRaw == null) {
            throw new IllegalArgumentException("You must provide at least one field to edit.");
        }

        String trimmedName = null;
        if (newName != null) {
            trimmedName = newName.trim();
            if (trimmedName.isBlank()) {
                throw new IllegalArgumentException("Event name cannot be blank.");
            }
        }

        Instant newScheduledAt = newScheduledAtRaw == null ? null : parseScheduledAt(newScheduledAtRaw);

        List<Long> replacementPlayerIds = null;
        if (playersRaw != null) {
            Set<Long> playerIds = parsePlayerIds(playersRaw);
            if (newGmUserId != null) {
                playerIds.add(newGmUserId);
            }
            replacementPlayerIds = new ArrayList<>(playerIds);
        }

        Integer newRecurrenceWeeks = newRecurrenceRaw == null ? null : parseRecurrenceWeeks(newRecurrenceRaw);

        return repository.updateEvent(
                guildId,
                eventId,
                actorUserId,
                authorizationService.isSuperadmin(actorUserId),
                newGmUserId,
                trimmedName,
                newScheduledAt,
                newRecurrenceWeeks,
                replacementPlayerIds
        );
    }

    public List<TtrpgWeekEntry> getCurrentWeekSchedule(long guildId) {
        List<TtrpgEventDetails> events = repository.findAllByGuildId(guildId);
        ZonedDateTime weekStart = LocalDate.now(INPUT_ZONE_ID)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(INPUT_ZONE_ID);
        ZonedDateTime weekEndExclusive = weekStart.plusWeeks(1);

        return events.stream()
                .map(event -> toWeekEntry(event, weekStart, weekEndExclusive))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .sorted(Comparator.comparing(TtrpgWeekEntry::occurrenceAt).thenComparing(TtrpgWeekEntry::id))
                .toList();
    }

    public boolean deleteEvent(long guildId, long eventId, long actorUserId) {
        return repository.deleteEvent(guildId, eventId, actorUserId, authorizationService.isSuperadmin(actorUserId));
    }

    public List<TtrpgEventDetails> getCampaignsForPlayer(long guildId, long userId) {
        return repository.findAllByGuildId(guildId).stream()
                .filter(event -> event.playerIds().contains(userId))
                .sorted(Comparator.comparing(TtrpgEventDetails::recurrenceWeeks).reversed()
                        .thenComparing(TtrpgEventDetails::scheduledAt)
                        .thenComparing(TtrpgEventDetails::id))
                .toList();
    }

    public String getTimeZoneName() {
        return INPUT_TIME_ZONE_LABEL;
    }

    private Instant parseScheduledAt(String scheduledAtRaw) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(scheduledAtRaw.trim(), DATE_TIME_FORMATTER);
            return localDateTime.atZone(INPUT_ZONE_ID).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid time. Use format `" + DATE_TIME_FORMAT + "` in `" + INPUT_TIME_ZONE_LABEL + "`.", exception);
        }
    }

    private Set<Long> parsePlayerIds(String playersRaw) {
        Set<Long> playerIds = new LinkedHashSet<>();
        if (playersRaw == null || playersRaw.isBlank()) {
            return playerIds;
        }

        Matcher matcher = USER_ID_PATTERN.matcher(playersRaw);
        while (matcher.find()) {
            playerIds.add(Long.parseLong(matcher.group()));
        }

        return playerIds;
    }

    private int parseRecurrenceWeeks(String recurrenceRaw) {
        if (recurrenceRaw == null || recurrenceRaw.isBlank() || RECURRENCE_NONE.equalsIgnoreCase(recurrenceRaw)) {
            return 0;
        }
        if (RECURRENCE_WEEKLY.equalsIgnoreCase(recurrenceRaw)) {
            return 1;
        }
        if (RECURRENCE_BIWEEKLY.equalsIgnoreCase(recurrenceRaw)) {
            return 2;
        }

        throw new IllegalArgumentException("Invalid recurrence. Use `none`, `weekly`, or `biweekly`.");
    }

    private java.util.Optional<TtrpgWeekEntry> toWeekEntry(TtrpgEventDetails event, ZonedDateTime weekStart, ZonedDateTime weekEndExclusive) {
        if (event.recurrenceWeeks() == 0) {
            if (event.scheduledAt().isBefore(weekStart.toInstant()) || !event.scheduledAt().isBefore(weekEndExclusive.toInstant())) {
                return java.util.Optional.empty();
            }

            return java.util.Optional.of(toWeekEntry(event, event.scheduledAt()));
        }

        ZonedDateTime sourceDateTime = event.scheduledAt().atZone(INPUT_ZONE_ID);
        ZonedDateTime sourceWeekStart = sourceDateTime.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(INPUT_ZONE_ID);
        long weeksBetween = ChronoUnit.WEEKS.between(sourceWeekStart.toLocalDate(), weekStart.toLocalDate());
        if (weeksBetween < 0 || weeksBetween % event.recurrenceWeeks() != 0) {
            return java.util.Optional.empty();
        }

        LocalDate occurrenceDate = weekStart.toLocalDate().with(TemporalAdjusters.nextOrSame(sourceDateTime.getDayOfWeek()));
        ZonedDateTime occurrenceAt = ZonedDateTime.of(occurrenceDate, LocalTime.from(sourceDateTime), INPUT_ZONE_ID);
        if (occurrenceAt.isBefore(weekStart) || !occurrenceAt.isBefore(weekEndExclusive)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(toWeekEntry(event, occurrenceAt.toInstant()));
    }

    private TtrpgWeekEntry toWeekEntry(TtrpgEventDetails event, Instant occurrenceAt) {
        return new TtrpgWeekEntry(
                event.id(),
                event.name(),
                occurrenceAt,
                event.recurrenceWeeks(),
                event.gmUserId(),
                event.playerIds()
        );
    }

}
