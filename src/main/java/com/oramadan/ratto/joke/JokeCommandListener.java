package com.oramadan.ratto.joke;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JokeCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JokeCommandListener.class);

    private static final String COMMAND_NAME = "joke";
    private static final String SUBCOMMAND_GET = "get";

    JokeService jokeService = new JokeService();

    // -------- Feature Bootstrap ---------

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("JokeCommandListener is ready and listening");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName()) || event.getGuild() == null || !(event.getChannel() instanceof GuildMessageChannel channel)) {
            return;
        }

        String subcommandName = event.getSubcommandName();
        if (SUBCOMMAND_GET.equals(subcommandName)) {
            handleGetJoke(event, channel);
            return;
        }

    }

    // --------- Joke Commands ---------

    private void handleGetJoke(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        try {
            String joke = jokeService.getJoke();
            event.reply(joke).setEphemeral(false).queue();
        } catch (IOException | InterruptedException e) {
            event.reply("Failed to get a joke from the joke service").setEphemeral(true).queue();
        }
    }

}
