package com.oramadan.ratto.currency;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyCommandListener extends ListenerAdapter {
    private static final int LEADERBOARD_SIZE = 10;

    private static final Logger logger = LoggerFactory.getLogger(CurrencyCommandListener.class);

    private static final String COMMAND_NAME = "chedda";
    private static final String SUBCOMMAND_CHECK = "check";
    private static final String SUBCOMMAND_SCAVENGE = "scavenge";
    private static final String SUBCOMMAND_LEADERBOARD = "leaderboard";

    private final CurrencyService currencyService;

    public CurrencyCommandListener(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    // -------- Feature Bootstrap --------

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("CurrencyCommandListener is ready and listening");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName()) || event.getGuild() == null || !(event.getChannel() instanceof GuildMessageChannel channel)) {
            return;
        }

        String subCommandName = event.getSubcommandName();
        if (SUBCOMMAND_CHECK.equals(subCommandName)) {
            handleCheddaCheck(event, channel);
            return;
        }

        if (SUBCOMMAND_SCAVENGE.equals(subCommandName)) {
            handleCheddaScavenge(event, channel);
            return;
        }

        if (SUBCOMMAND_LEADERBOARD.equals(subCommandName)) {
            handleCheddaLeaderboard(event, channel);
        }

    }

    // -------- Chedda Commands --------
    private void handleCheddaCheck(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        long userId = event.getUser().getIdLong();

        event.deferReply().queue();
        int chedda = currencyService.getCheddaFor(userId);
        event.getHook()
                .sendMessage(event.getUser().getAsMention() + " has **" + chedda + " \uD83E\uDDC0**")
                .queue();
    }

    private void handleCheddaScavenge(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        long userId = event.getUser().getIdLong();

        event.deferReply().queue();

        CurrencyScavengeResult result = currencyService.scavenge(userId);
        if (result.rateLimited()) {
            event.getHook()
                    .sendMessage(event.getUser().getAsMention() + " has already scavenged 3 times in the last hour.")
                    .queue();
            return;
        }

        int totalChedda = currencyService.getCheddaFor(userId);

        String rewardMessage = result.awardedChedda() > 0
                ? "found **" + result.awardedChedda() + " \uD83E\uDDC0**."
                : "found **no chedda**.";

        event.getHook()
                .sendMessage(event.getUser().getAsMention() + " " + rewardMessage
                        + " You now have **" + totalChedda + " \uD83E\uDDC0**."
                        + " `" + result.attemptsRemaining() + "/3` scavenges remaining this hour.")
                .queue();
    }

    private void handleCheddaLeaderboard(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        event.deferReply().queue();

        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().sendMessage("This command can only be used in a server.").queue();
            return;
        }

        String leaderboardMessage = buildLeaderboardMessage(guild, event.getUser().getIdLong());
        event.getHook().sendMessage(leaderboardMessage).queue();
    }

    private String buildLeaderboardMessage(Guild guild, long requestingUserId) {
        var leaderboard = currencyService.getLeaderboard();
        if (leaderboard.isEmpty()) {
            return "No chedda leaderboard entries yet.";
        }

        StringBuilder builder = new StringBuilder("**Chedda Leaderboard**\n\n");
        int topCount = Math.min(LEADERBOARD_SIZE, leaderboard.size());

        for (int index = 0; index < topCount; index++) {
            CurrencyLeaderboardEntry entry = leaderboard.get(index);
            builder.append(getRankPrefix(index + 1))
                    .append(" ")
                    .append(formatUser(entry.userId(), guild))
                    .append(" - **")
                    .append(entry.chedda())
                    .append(" \uD83E\uDDC0**")
                    .append("\n");
        }

        int requesterRank = findRankForUser(leaderboard, requestingUserId);
        if (requesterRank > LEADERBOARD_SIZE) {
            CurrencyLeaderboardEntry requesterEntry = leaderboard.get(requesterRank - 1);
            builder.append("\n")
                    .append("Your rank: ")
                    .append(getRankPrefix(requesterRank))
                    .append(" ")
                    .append(formatUser(requesterEntry.userId(), guild))
                    .append(" - **")
                    .append(requesterEntry.chedda())
                    .append(" \uD83E\uDDC0**");
        }

        return builder.toString().trim();
    }

    private int findRankForUser(java.util.List<CurrencyLeaderboardEntry> leaderboard, long userId) {
        for (int index = 0; index < leaderboard.size(); index++) {
            if (leaderboard.get(index).userId() == userId) {
                return index + 1;
            }
        }

        return -1;
    }

    private String getRankPrefix(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "`#" + rank + "`";
        };
    }

    private String formatUser(long userId, Guild guild) {
        if (guild.getMemberById(userId) != null) {
            return guild.getMemberById(userId).getAsMention();
        }

        return "<@" + userId + ">";
    }

}
