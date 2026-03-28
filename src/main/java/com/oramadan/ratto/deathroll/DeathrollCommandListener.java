package com.oramadan.ratto.deathroll;

import com.oramadan.ratto.currency.CurrencyService;
import com.oramadan.ratto.deathroll.dto.DeathrollChallenge;
import com.oramadan.ratto.deathroll.dto.DeathrollRollResult;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeathrollCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DeathrollCommandListener.class);

    private static final String COMMAND_NAME = "deathroll";
    private static final String SUBCOMMAND_CHALLENGE = "challenge";
    private static final String SUBCOMMAND_ACCEPT = "accept";
    private static final String SUBCOMMAND_DECLINE = "decline";
    private static final String ROLL_BUTTON_PREFIX = "deathroll:roll:";

    private static final long GAME_TIMEOUT_HOURS = 1;

    private final CurrencyService currencyService;
    private final DeathrollService deathrollService = new DeathrollService();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DeathrollCommandListener(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    // -------- Feature Bootstrap --------

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("DeathrollCommandListener is ready and listening");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName()) || event.getGuild() == null || !(event.getChannel() instanceof GuildMessageChannel channel)) {
            return;
        }

        String subcommandName = event.getSubcommandName();
        if (SUBCOMMAND_CHALLENGE.equals(subcommandName)) {
            handleChallenge(event, channel);
            return;
        }

        if (SUBCOMMAND_ACCEPT.equals(subcommandName)) {
            handleAccept(event, channel);
            return;
        }

        if (SUBCOMMAND_DECLINE.equals(subcommandName)) {
            handleDecline(event, channel);
        }
    }

    // -------- Challenge Commands --------

    private void handleChallenge(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        OptionMapping userOption = event.getOption("user");
        if (userOption == null || userOption.getAsUser().isBot()) {
            event.reply("You must pick a valid user to challenge.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long challengerId = event.getUser().getIdLong();
        long challengedId = userOption.getAsUser().getIdLong();
        int startingMaximum = getStartingMaximum(event.getOption("max"));
        int wagerChedda = getWagerChedda(event.getOption("wager"));

        if (challengerId == challengedId) {
            event.reply("You cannot challenge yourself.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (startingMaximum < 2) {
            event.reply("The starting max must be at least 2.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (wagerChedda < 0) {
            event.reply("The wager cannot be negative.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue(hook -> channel.sendMessage(
                buildChallengeMessage(challengerId, challengedId, startingMaximum, wagerChedda)
        ).queue(message -> {
            boolean created = deathrollService.createChallenge(
                    event.getGuild().getIdLong(),
                    channel.getIdLong(),
                    message.getIdLong(),
                    challengerId,
                    challengedId,
                    startingMaximum,
                    wagerChedda
            ).isPresent();

            if (!created) {
                message.delete().queue();
                hook.editOriginal("That deathroll challenge is invalid or one of those users is already in a pending or active deathroll.").queue();
                return;
            }

            hook.deleteOriginal().queue();
        }, failure -> hook.editOriginal("I could not post the deathroll challenge in this channel.").queue()));
    }

    private void handleAccept(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        event.deferReply(true).queue(hook -> handleAcceptDeferred(event, channel, hook));
    }

    private void handleAcceptDeferred(SlashCommandInteractionEvent event, GuildMessageChannel channel, InteractionHook hook) {
        long challengedId = event.getUser().getIdLong();
        DeathrollChallenge challenge = deathrollService.removeChallenge(
                event.getGuild().getIdLong(),
                channel.getIdLong(),
                challengedId
        ).orElse(null);

        if (challenge == null) {
            hook.editOriginal("You do not have a pending deathroll challenge in this channel.").queue();
            return;
        }

        String wagerFailure = collectWagerIfNeeded(challenge);
        if (wagerFailure != null) {
            channel.retrieveMessageById(challenge.messageId()).queue(
                    challengeMessage -> challengeMessage.delete().queue(),
                    failure -> {
                    }
            );
            hook.editOriginal(wagerFailure).queue();
            return;
        }

        channel.retrieveMessageById(challenge.messageId()).queue(challengeMessage -> {
            String challengerName = event.getGuild().getMemberById(challenge.challengerId()) != null
                    ? event.getGuild().getMemberById(challenge.challengerId()).getEffectiveName()
                    : Long.toString(challenge.challengerId());
            String challengedName = event.getMember() != null
                    ? event.getMember().getEffectiveName()
                    : event.getUser().getName();
            String threadName = "deathroll-" + sanitizeThreadName(challengerName) + "-vs-" + sanitizeThreadName(challengedName);
            challengeMessage.createThreadChannel(threadName).queue(threadChannel -> {
                DeathrollGame game = deathrollService.startGame(threadChannel.getIdLong(), challenge);
                challengeMessage.delete().queue(
                        success -> {
                        },
                        failure -> {
                        }
                );
                threadChannel.sendMessage(buildGameStartMessage(challenge, game))
                        .setComponents(ActionRow.of(Button.primary(ROLL_BUTTON_PREFIX + threadChannel.getIdLong(), "Roll"))).queue(message -> {
                    game.setActivePromptMessageId(message.getIdLong());
                    threadChannel.addThreadMemberById(challenge.challengerId()).queue();
                    threadChannel.addThreadMemberById(challenge.challengedId()).queue();
                    scheduleGameTimeout(threadChannel.getIdLong(), threadChannel);
                    hook.deleteOriginal().queue();
                });
            }, failure -> hook.editOriginal("I could not create the deathroll thread here.").queue());
        }, failure -> hook.editOriginal("The original challenge message could not be found.").queue());
    }

    private void handleDecline(SlashCommandInteractionEvent event, GuildMessageChannel channel) {
        long challengedId = event.getUser().getIdLong();
        DeathrollChallenge challenge = deathrollService.removeChallenge(
                event.getGuild().getIdLong(),
                channel.getIdLong(),
                challengedId
        ).orElse(null);

        if (challenge == null) {
            event.reply("You do not have a pending deathroll challenge in this channel.").setEphemeral(true).queue();
            return;
        }

        channel.retrieveMessageById(challenge.messageId()).queue(
                challengeMessage -> challengeMessage.delete().queue(),
                failure -> {
                }
        );

        event.reply(buildDeclineMessage(challengedId, challenge.challengerId()))
                .queue();
    }

    // -------- Game Interaction --------

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith(ROLL_BUTTON_PREFIX) || !(event.getChannel() instanceof ThreadChannel threadChannel)) {
            return;
        }

        long threadId = Long.parseLong(componentId.substring(ROLL_BUTTON_PREFIX.length()));
        long userId = event.getUser().getIdLong();
        long promptMessageId = event.getMessageIdLong();

        DeathrollGame game = deathrollService.findGame(threadId).orElse(null);
        if (game == null) {
            event.reply("That deathroll game is no longer active.").setEphemeral(true).queue();
            disableRollPrompt(event);
            return;
        }

        if (game.getCurrentTurnUserId() != userId) {
            event.reply("It is not your turn.").setEphemeral(true).queue();
            return;
        }

        if (game.getActivePromptMessageId() != promptMessageId) {
            event.reply("Use the latest roll prompt in this thread.").setEphemeral(true).queue();
            return;
        }

        deathrollService.roll(threadId, userId, promptMessageId).ifPresentOrElse(result -> {
            disableRollPrompt(event);

            // Game over
            if (result.gameOver()) {
                long winnerUserId = game.otherPlayer(result.losingUserId());
                payoutWinner(game, winnerUserId);

                threadChannel.sendMessage(buildGameOverMessage(result, winnerUserId, game.getWagerChedda()))
                        .queue(message -> threadChannel.delete().queueAfter(20, TimeUnit.SECONDS));

                return;
            }

            // Next turn
            threadChannel.sendMessage(buildNextRollMessage(userId, result))
                    .setComponents(ActionRow.of(Button.primary(ROLL_BUTTON_PREFIX + threadId, "Roll")))
                    .queue(message -> game.setActivePromptMessageId(message.getIdLong()) );

        }, () -> event.reply("That roll could not be processed.").setEphemeral(true).queue());
    }

    // -------- Utility Helpers --------

    private void disableRollPrompt(ButtonInteractionEvent event) {
        event.editComponents().queue();
    }

    private void scheduleGameTimeout(long threadId, ThreadChannel threadChannel) {
        scheduler.schedule(() -> {
            DeathrollGame game = deathrollService.removeGame(threadId).orElse(null);
            if (game == null) {
                return;
            }

            refundWager(game);
            threadChannel.delete().queue(
                    success -> {
                    },
                    failure -> {
                    }
            );
        }, GAME_TIMEOUT_HOURS, TimeUnit.HOURS);
    }

    private String buildChallengeMessage(long challengerUserId, long challengedUserId, int startingMaximum, int wagerChedda) {
        String wagerLine = wagerChedda > 0
                ? "Wager: **" + wagerChedda + " chedda** each."
                : "Wager: **none**.";

        return """
                %s has challenged %s to a deathroll.
                Starting range: `1-%d`.
                %s
                Use `/deathroll accept` or `/deathroll decline`.
                """.formatted(
                mentionUser(challengerUserId),
                mentionUser(challengedUserId),
                startingMaximum,
                wagerLine
        );
    }

    private String buildGameStartMessage(DeathrollChallenge challenge, DeathrollGame game) {
        String wagerLine = challenge.wagerChedda() > 0
                ? "Wager locked: **" + challenge.wagerChedda() + " chedda** each."
                : "No chedda wager for this match.";

        return """
                Deathroll started between %s and %s.

                Starting range: `1-%d`.

                %s

                %s rolls first.
                """.formatted(
                mentionUser(challenge.challengerId()),
                mentionUser(challenge.challengedId()),
                game.getStartingMaximum(),
                wagerLine,
                mentionUser(game.getCurrentTurnUserId())
        );
    }

    private String buildGameOverMessage(DeathrollRollResult result, long winnerUserId, int wagerChedda) {
        String payoutLine = wagerChedda > 0
                ? "%s wins **%d chedda**.".formatted(mentionUser(winnerUserId), wagerChedda * 2)
                : "%s wins the deathroll.".formatted(mentionUser(winnerUserId));

        return """
                %s rolled **1** out of **%d** and loses.

                %s

                This thread will be deleted in 20 seconds.
                """.formatted(
                mentionUser(result.losingUserId()),
                result.previousMaximum(),
                payoutLine
        );
    }

    private String buildNextRollMessage(long userId, DeathrollRollResult result) {
        return """
                %s rolled **%d** out of **%d**.
                """.formatted(
                mentionUser(userId),
                result.rolledValue(),
                result.previousMaximum(),
                mentionUser(result.nextTurnUserId()),
                result.nextMaximum()
        );
    }

    private String buildDeclineMessage(long challengedUserId, long challengerUserId) {
        return "%s declined %s's deathroll challenge."
                .formatted(
                        mentionUser(challengedUserId),
                        mentionUser(challengerUserId)
                );
    }

    private int getStartingMaximum(OptionMapping option) {
        if (option == null) {
            return DeathrollService.DEFAULT_STARTING_MAXIMUM;
        }

        return option.getAsInt();
    }

    private int getWagerChedda(OptionMapping option) {
        if (option == null) {
            return 0;
        }

        return option.getAsInt();
    }

    private String collectWagerIfNeeded(DeathrollChallenge challenge) {
        int wagerChedda = challenge.wagerChedda();
        if (wagerChedda == 0) {
            return null;
        }

        if (!currencyService.hasChedda(challenge.challengerId(), wagerChedda)) {
            return mentionUser(challenge.challengerId()) + " no longer has enough chedda to cover the wager.";
        }

        if (!currencyService.hasChedda(challenge.challengedId(), wagerChedda)) {
            return mentionUser(challenge.challengedId()) + " does not have enough chedda to accept that wager.";
        }

        currencyService.removeChedda(challenge.challengerId(), wagerChedda);
        try {
            currencyService.removeChedda(challenge.challengedId(), wagerChedda);
        } catch (RuntimeException exception) {
            currencyService.addChedda(challenge.challengerId(), wagerChedda);
            throw exception;
        }

        return null;
    }

    private void payoutWinner(DeathrollGame game, long winnerUserId) {
        if (game.getWagerChedda() == 0) {
            return;
        }

        currencyService.addChedda(winnerUserId, game.getWagerChedda() * 2);
    }

    private void refundWager(DeathrollGame game) {
        if (game.getWagerChedda() == 0) {
            return;
        }

        currencyService.addChedda(game.getChallengerId(), game.getWagerChedda());
        currencyService.addChedda(game.getChallengedId(), game.getWagerChedda());
    }

    private String sanitizeThreadName(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    private String mentionUser(long userId) {
        return "<@" + userId + ">";
    }
}
