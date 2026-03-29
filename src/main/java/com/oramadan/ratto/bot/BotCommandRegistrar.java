package com.oramadan.ratto.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class BotCommandRegistrar {

    public static void register(JDA jda) {
        OptionData ttrpgRecurrenceOption = new OptionData(OptionType.STRING, "recurrence", "Recurrence: none, weekly, or biweekly", false)
                .addChoice("None", "none")
                .addChoice("Weekly", "weekly")
                .addChoice("Biweekly", "biweekly");

        jda.updateCommands()
                .addCommands(
                        // Currency
                        Commands.slash("chedda", "Manage your chedda")
                                .addSubcommands(
                                        new SubcommandData("check", "Check your chedda"),
                                        new SubcommandData("scavenge", "Try to scavenge some chedda"),
                                        new SubcommandData("leaderboard", "Show the chedda board")
                                ),
                        // Deathroll
                        Commands.slash("deathroll", "Play a WoW-style deathroll game")
                                .addSubcommands(
                                        new SubcommandData("challenge", "Challenge another user")
                                                .addOption(OptionType.USER, "user", "The user to challenge", true)
                                                .addOption(OptionType.INTEGER, "max", "S\tarting roll maximum (defaults to 100)")
                                                .addOption(OptionType.INTEGER, "wager", "Chedda wager per player (defaults to 0)"),
                                        new SubcommandData("accept", "Accept a pending deathroll challenge")
                                                .addOption(OptionType.USER, "user", "The user who challenged you", true),
                                        new SubcommandData("decline", "Decline a pending deathroll challenge")
                                                .addOption(OptionType.USER, "user", "The user whose challenge you are declining", true)
                                ),
                        // TTRPG
                        Commands.slash("ttrpg", "Manage TTRPG sessions")
                                .addSubcommands(
                                        new SubcommandData("create", "Create a TTRPG session")
                                                .addOption(OptionType.STRING, "name", "Event name", true)
                                                .addOption(OptionType.STRING, "when", "When to play in GMT+0, format: yyyy-MM-dd HH:mm", true)
                                                .addOption(OptionType.USER, "gm", "The game master for this event", true)
                                                .addOptions(ttrpgRecurrenceOption)
                                                .addOption(OptionType.STRING, "players", "Mention or paste player ids separated by spaces"),
                                        new SubcommandData("edit", "Edit one of your TTRPG sessions")
                                                .addOption(OptionType.INTEGER, "id", "The event id", true)
                                                .addOption(OptionType.STRING, "name", "Updated event name")
                                                .addOption(OptionType.STRING, "when", "Updated time in GMT+0, format: yyyy-MM-dd HH:mm")
                                                .addOption(OptionType.USER, "gm", "Updated game master")
                                                .addOptions(ttrpgRecurrenceOption)
                                                .addOption(OptionType.STRING, "players", "Replacement player list via mentions or ids"),
                                        new SubcommandData("campaigns", "Show a player's TTRPG campaigns")
                                                .addOption(OptionType.USER, "user", "The player to inspect"),
                                        new SubcommandData("week", "Show this week's TTRPG schedule"),
                                        new SubcommandData("delete", "Delete one of your TTRPG sessions")
                                                .addOption(OptionType.INTEGER, "id", "The event id", true)
                                ),
                        // Jokes
                        Commands.slash("joke", "Fetched straight from Nizar's mind!")
                                .addSubcommands(
                                        new SubcommandData("get", "Get a new joke")
                                ))
                .queue();
    }
}
