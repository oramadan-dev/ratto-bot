package com.oramadan.ratto.ttrpg;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TtrpgCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TtrpgCommandListener.class);

    private static final String COMMAND_NAME = "ttrpg";
    private static final String SUBCOMMAND_CREATE = "create";
    private static final String SUBCOMMAND_EDIT = "edit";
    private static final String SUBCOMMAND_WEEK = "week";
    private static final String SUBCOMMAND_CAMPAIGNS = "campaigns";
    private static final String SUBCOMMAND_DELETE = "delete";

    private final TtrpgService ttrpgService;

    public TtrpgCommandListener(TtrpgService ttrpgService) {
        this.ttrpgService = ttrpgService;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("TtrpgCommandListener is ready and listening");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName()) || event.getGuild() == null || !(event.getChannel() instanceof GuildMessageChannel channel)) {
            return;
        }

        String subcommandName = event.getSubcommandName();
        if (SUBCOMMAND_CREATE.equals(subcommandName)) {
            handleCreate(event, channel);
            return;
        }

        if (SUBCOMMAND_WEEK.equals(subcommandName)) {
            handleWeek(event, channel);
            return;
        }

        if (SUBCOMMAND_EDIT.equals(subcommandName)) {
            handleEdit(event, channel);
            return;
        }

        if (SUBCOMMAND_CAMPAIGNS.equals(subcommandName)) {
            handleCampaigns(event, channel);
            return;
        }

        if (SUBCOMMAND_DELETE.equals(subcommandName)) {
            handleDelete(event, channel);
        }
    }

    private void handleCreate(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        OptionMapping nameOption = event.getOption("name");
        OptionMapping whenOption = event.getOption("when");
        OptionMapping gmOption = event.getOption("gm");
        if (nameOption == null || whenOption == null || gmOption == null || gmOption.getAsUser().isBot()) {
            event.reply("You must provide an event name, time in `" + TtrpgService.INPUT_TIME_ZONE_LABEL + "`, and a valid GM.").setEphemeral(true).queue();
            return;
        }

        String playersRaw = event.getOption("players", OptionMapping::getAsString);
        String recurrence = event.getOption("recurrence", TtrpgService.RECURRENCE_NONE, OptionMapping::getAsString);

        try {
            TtrpgEventDetails createdEvent = ttrpgService.createEvent(
                    event.getGuild().getIdLong(),
                    gmOption.getAsUser().getIdLong(),
                    nameOption.getAsString(),
                    whenOption.getAsString(),
                    recurrence,
                    playersRaw
            );

            event.deferReply().queue(hook -> resolveDisplayNames(event.getGuild(), collectUserIds(createdEvent), displayNames ->
                    hook.sendMessage(buildCreateMessage(createdEvent, displayNames, ttrpgService.getTimeZoneName())).queue()));
        } catch (IllegalArgumentException exception) {
            event.reply(exception.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleWeek(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        List<TtrpgWeekEntry> entries = ttrpgService.getCurrentWeekSchedule(event.getGuild().getIdLong());
        event.deferReply().queue(hook -> resolveDisplayNames(event.getGuild(), collectUserIds(entries), displayNames ->
                hook.sendMessage(buildWeekMessage(entries, displayNames, ttrpgService.getTimeZoneName())).queue()));
    }

    private void handleEdit(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        OptionMapping idOption = event.getOption("id");
        if (idOption == null) {
            event.reply("You must provide an event id to edit.").setEphemeral(true).queue();
            return;
        }

        String newName = event.getOption("name", OptionMapping::getAsString);
        String newWhen = event.getOption("when", OptionMapping::getAsString);
        OptionMapping gmOption = event.getOption("gm");
        Long newGmUserId = gmOption == null ? null : gmOption.getAsUser().getIdLong();
        String newRecurrence = event.getOption("recurrence", OptionMapping::getAsString);
        String newPlayers = event.getOption("players", OptionMapping::getAsString);

        try {
            TtrpgEventDetails updatedEvent = ttrpgService.editEvent(
                    event.getGuild().getIdLong(),
                    idOption.getAsLong(),
                    event.getUser().getIdLong(),
                    newGmUserId,
                    newName,
                    newWhen,
                    newRecurrence,
                    newPlayers
            ).orElse(null);

            if (updatedEvent == null) {
                event.reply("I could not edit that event. Make sure it exists in this server and that you are its current GM.").setEphemeral(true).queue();
                return;
            }

            event.deferReply().queue(hook -> resolveDisplayNames(event.getGuild(), collectUserIds(updatedEvent), displayNames ->
                    hook.sendMessage(buildEditMessage(updatedEvent, displayNames, ttrpgService.getTimeZoneName())).queue()));
        } catch (IllegalArgumentException exception) {
            event.reply(exception.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleDelete(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        OptionMapping idOption = event.getOption("id");
        if (idOption == null) {
            event.reply("You must provide an event id to delete.").setEphemeral(true).queue();
            return;
        }

        boolean deleted = ttrpgService.deleteEvent(
                event.getGuild().getIdLong(),
                idOption.getAsLong(),
                event.getUser().getIdLong()
        );

        if (!deleted) {
            event.reply("I could not delete that event. Make sure it exists in this server and that you are its GM.").setEphemeral(true).queue();
            return;
        }

        event.reply("Deleted TTRPG event `" + idOption.getAsLong() + "`.").queue();
    }

    private void handleCampaigns(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        long targetUserId = event.getOption("user", event.getUser().getIdLong(), OptionMapping::getAsLong);
        List<TtrpgEventDetails> events = ttrpgService.getCampaignsForPlayer(event.getGuild().getIdLong(), targetUserId);
        event.deferReply().queue(hook -> resolveDisplayNames(event.getGuild(), collectUserIds(events, targetUserId), displayNames ->
                hook.sendMessage(buildCampaignsMessage(events, targetUserId, displayNames, ttrpgService.getTimeZoneName())).queue()));
    }

    private String buildCreateMessage(TtrpgEventDetails event, Map<Long, String> displayNames, String timeZoneName) {
        return """
                Created TTRPG event `%d`: **%s**

                When: %s
                Recurrence: **%s**
                GM: %s
                Players:
                %s
                Input time is interpreted in `%s`. Discord displays timestamps in each viewer's local timezone.
                """.formatted(
                event.id(),
                event.name(),
                formatDiscordTimestamp(event.scheduledAt()),
                formatRecurrence(event.recurrenceWeeks()),
                formatDisplayName(event.gmUserId(), displayNames),
                formatPlayerNames(event.playerIds(), displayNames),
                timeZoneName
        );
    }

    private String buildEditMessage(TtrpgEventDetails event, Map<Long, String> displayNames, String timeZoneName) {
        return """
                Updated TTRPG event `%d`: **%s**

                When: %s
                Recurrence: **%s**
                GM: %s
                Players:
                %s
                Input time is interpreted in `%s`. Discord displays timestamps in each viewer's local timezone.
                """.formatted(
                event.id(),
                event.name(),
                formatDiscordTimestamp(event.scheduledAt()),
                formatRecurrence(event.recurrenceWeeks()),
                formatDisplayName(event.gmUserId(), displayNames),
                formatPlayerNames(event.playerIds(), displayNames),
                timeZoneName
        );
    }

    private String buildWeekMessage(List<TtrpgWeekEntry> entries, Map<Long, String> displayNames, String timeZoneName) {
        if (entries.isEmpty()) {
            return "No TTRPG sessions scheduled for this week. Input time is interpreted in `" + timeZoneName + "`. Discord displays timestamps in each viewer's local timezone.";
        }

        String body = entries.stream()
                .map(entry -> """
                        `%d` **%s**
                        %s%s
                        GM: %s
                        Players:
                        %s
                        """.formatted(
                        entry.id(),
                        entry.name(),
                        formatDiscordTimestamp(entry.occurrenceAt()),
                        formatRecurrenceSuffix(entry.recurrenceWeeks()),
                        formatDisplayName(entry.gmUserId(), displayNames),
                        formatPlayerNames(entry.playerIds(), displayNames)
                ))
                .collect(Collectors.joining("\n"));

        return """
                **TTRPG This Week**
                Input time is interpreted in `%s`. Discord displays timestamps in each viewer's local timezone.

                %s
                """.formatted(timeZoneName, body);
    }

    private String buildCampaignsMessage(List<TtrpgEventDetails> events, long userId, Map<Long, String> displayNames, String timeZoneName) {
        if (events.isEmpty()) {
            return formatDisplayName(userId, displayNames) + " is not in any TTRPG campaigns for this server. Input time is interpreted in `" + timeZoneName + "`. Discord displays timestamps in each viewer's local timezone.";
        }

        String body = events.stream()
                .map(event -> """
                        `%d` **%s**
                        %s%s
                        GM: %s
                        Players:
                        %s
                        """.formatted(
                        event.id(),
                        event.name(),
                        formatDiscordTimestamp(event.scheduledAt()),
                        formatRecurrenceSuffix(event.recurrenceWeeks()),
                        formatDisplayName(event.gmUserId(), displayNames),
                        formatPlayerNames(event.playerIds(), displayNames)
                ))
                .collect(Collectors.joining("\n"));

        return """
                **TTRPG Campaigns For %s**
                Input time is interpreted in `%s`. Discord displays timestamps in each viewer's local timezone.

                %s
                """.formatted(formatDisplayName(userId, displayNames), timeZoneName, body);
    }

    private String formatPlayerNames(List<Long> playerIds, Map<Long, String> displayNames) {
        return playerIds.stream()
                .map(userId -> formatDisplayName(userId, displayNames))
                .map(name -> "- " + name)
                .collect(Collectors.joining("\n"));
    }

    private String formatDisplayName(long userId, Map<Long, String> displayNames) {
        return displayNames.getOrDefault(userId, Long.toString(userId));
    }

    private String formatDiscordTimestamp(java.time.Instant instant) {
        return "<t:" + instant.getEpochSecond() + ":F>";
    }

    private String formatRecurrence(int recurrenceWeeks) {
        return switch (recurrenceWeeks) {
            case 0 -> "none";
            case 1 -> "weekly";
            case 2 -> "biweekly";
            default -> "every " + recurrenceWeeks + " weeks";
        };
    }

    private String formatRecurrenceSuffix(int recurrenceWeeks) {
        if (recurrenceWeeks == 0) {
            return "";
        }

        return " (" + formatRecurrence(recurrenceWeeks) + ")";
    }

    private void resolveDisplayNames(Guild guild, Set<Long> userIds, Consumer<Map<Long, String>> consumer) {
        Map<Long, String> displayNames = new HashMap<>();
        List<Long> missingUserIds = new ArrayList<>();

        for (Long userId : userIds) {
            if (guild.getMemberById(userId) != null) {
                displayNames.put(userId, guild.getMemberById(userId).getEffectiveName());
            } else {
                missingUserIds.add(userId);
            }
        }

        if (missingUserIds.isEmpty()) {
            consumer.accept(displayNames);
            return;
        }

        resolveMissingDisplayNames(guild, missingUserIds, 0, displayNames, consumer);
    }

    private Set<Long> collectUserIds(TtrpgEventDetails event) {
        Set<Long> userIds = new LinkedHashSet<>(event.playerIds());
        userIds.add(event.gmUserId());
        return userIds;
    }

    private Set<Long> collectUserIds(List<TtrpgWeekEntry> entries) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (TtrpgWeekEntry entry : entries) {
            userIds.add(entry.gmUserId());
            userIds.addAll(entry.playerIds());
        }
        return userIds;
    }

    private Set<Long> collectUserIds(List<TtrpgEventDetails> events, long targetUserId) {
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(targetUserId);
        for (TtrpgEventDetails event : events) {
            userIds.add(event.gmUserId());
            userIds.addAll(event.playerIds());
        }
        return userIds;
    }

    private void resolveMissingDisplayNames(
            Guild guild,
            List<Long> missingUserIds,
            int index,
            Map<Long, String> displayNames,
            Consumer<Map<Long, String>> consumer
    ) {
        if (index >= missingUserIds.size()) {
            consumer.accept(displayNames);
            return;
        }

        long userId = missingUserIds.get(index);
        guild.retrieveMemberById(userId).queue(member -> {
            displayNames.put(userId, member.getEffectiveName());
            resolveMissingDisplayNames(guild, missingUserIds, index + 1, displayNames, consumer);
        }, failure -> {
            displayNames.putIfAbsent(userId, Long.toString(userId));
            resolveMissingDisplayNames(guild, missingUserIds, index + 1, displayNames, consumer);
        });
    }
}
