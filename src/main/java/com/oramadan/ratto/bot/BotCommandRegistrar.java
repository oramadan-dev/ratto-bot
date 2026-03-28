package com.oramadan.ratto.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class BotCommandRegistrar {

    public static void register(JDA jda) {
        jda.updateCommands()
                .addCommands(
                        // Currency
                        Commands.slash("chedda", "Manage your chedda")
                                .addSubcommands(
                                        new SubcommandData("check", "Check your chedda")
                                ),
                        // Deathroll
                        Commands.slash("deathroll", "Play a WoW-style deathroll game")
                                .addSubcommands(
                                        new SubcommandData("challenge", "Challenge another user")
                                                .addOption(OptionType.USER, "user", "The user to challenge", true),
                                        new SubcommandData("accept", "Accept a pending deathroll challenge"),
                                        new SubcommandData("decline", "Decline a pending deathroll challenge")
                                ))
                .queue();
    }
}
