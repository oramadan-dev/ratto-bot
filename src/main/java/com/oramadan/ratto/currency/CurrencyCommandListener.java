package com.oramadan.ratto.currency;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyCommandListener.class);

    private static final String COMMAND_NAME = "chedda";
    private static final String SUBCOMMAND_CHECK = "check";

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

    }

    // -------- Chedda Commands --------
    private void handleCheddaCheck(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        long userId = event.getUser().getIdLong();

        event.deferReply().queue();
        int chedda = currencyService.getCheddaFor(userId);
        event.getHook()
                .sendMessage("You have **" + chedda + " \uD83E\uDDC0 **")
                .queue();
    }

}
