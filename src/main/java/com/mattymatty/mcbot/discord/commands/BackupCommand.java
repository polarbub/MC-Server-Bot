package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.Main;
import com.mattymatty.mcbot.backup.GitWrapper;
import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.discord.Listener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;
import org.eclipse.jgit.revwalk.RevCommit;

import java.text.DateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BackupCommand implements Command {
    private final Bot bot;
    private final GitWrapper git;
    private Timer actionTimer = new Timer();

    public BackupCommand(Bot bot) {
        this.bot = bot;
        this.git = Main.GIT;
    }

    @Override
    public String getName() {
        return "backup";
    }

    @Override
    public String getDescription() {
        return "handles backups";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName() + " list", "shows a list of current saved backups", false);
        eb.addField(getName() + " save (comment)", "performs a backup", false);
        eb.addField(getName() + " restore (rev_id)", "restores a backup\nif no RefID is provided shows a interactive menu to choose from", false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(), getDescription()).addSubcommands(
                new SubcommandData("list","shows a list of current saved backups"),
                new SubcommandData("save","performs a backup").addOption(OptionType.STRING,"comment","the name of the backup",false),
                new SubcommandData("restore","restores a backup").addOption(OptionType.STRING,"rev_id","the id of the commit to rollback to")
        );
    }

    @Override
    public void run(SlashCommandEvent event) {
        if(event.getSubcommandName() != null) {
            event.deferReply().queue();
            switch (event.getSubcommandName()) {
                case "list" -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Backup");
                    List<RevCommit> backups = git.getBackups();
                    int pages = (int) Math.ceil((float) (backups.size()) / 5);
                    eb.setDescription((backups.size() > 0) ? "Listing last available backups\r\nPage: " + 1 + "/" + pages : "No backups found");
                    backups.stream().limit(5).forEach(b -> {
                        eb.addField(b.getName().substring(0, 7),
                                "date:\t " + DateFormat.getDateTimeInstance().format(b.getAuthorIdent().getWhen()) +
                                        "\ncomment:\t " + b.getFullMessage(), false);
                    });
                    Button up = Button.primary(event.getInteraction().getId() + ":page" + String.valueOf(0), "Previous").asDisabled();
                    Button down = Button.primary(event.getInteraction().getId() + ":page" + String.valueOf(1), "Next");
                    if (1 > pages - 1)
                        down = down.asDisabled();
                    event.getHook().sendMessageEmbeds(eb.build())
                            .addActionRow(up, down).queue();
                    Listener.BtnEventListener listener = (e) -> {
                        try {
                            String page = e.getComponentId().split(":")[1].split("page")[1];
                            int i = Integer.parseInt(page);
                            int skip = i * 5;
                            e.deferEdit().queue();
                            EmbedBuilder eb2 = new EmbedBuilder();
                            eb2.setTitle("Backup");
                            eb2.setDescription((backups.size() > 0) ? "Listing last available backups\r\nPage: " + String.valueOf(i + 1) + "/" + pages : "No backups found");
                            backups.stream().skip(skip).limit(5).forEach(b -> {
                                eb2.addField(b.getName().substring(0, 7),
                                        "date:\t " + DateFormat.getDateTimeInstance().format(b.getAuthorIdent().getWhen()) +
                                                "\ncomment:\t " + b.getFullMessage(), false);
                            });
                            Button up2 = Button.primary(event.getInteraction().getId() + ":page" + String.valueOf(i - 1), "Previous");
                            Button down2 = Button.primary(event.getInteraction().getId() + ":page" + String.valueOf(i + 1), "Next");
                            if (i - 1 < 0)
                                up2 = up2.asDisabled();
                            if (i + 1 > pages - 1)
                                down2 = down2.asDisabled();
                            e.getHook().editOriginalEmbeds(eb2.build()).queue();
                            e.getHook().editOriginalComponents(ActionRow.of(up2, down2)).queue();
                            actionTimer.cancel();
                            actionTimer = new Timer();
                            actionTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    bot.listener.btnListeners.remove(event.getInteraction().getId());
                                }
                            },5000);
                        } catch (NumberFormatException ignored) {
                        }
                    };
                    actionTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            bot.listener.btnListeners.remove(event.getInteraction().getId());
                        }
                    },5000);
                    bot.listener.btnListeners.put(event.getInteraction().getId(), listener);
                }
                case "save" -> {
                }
                case "restore" -> {
                }
                default -> {

                }
            }
        }
    }


}
