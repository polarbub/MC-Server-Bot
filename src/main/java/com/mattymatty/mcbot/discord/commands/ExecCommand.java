package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;

public class ExecCommand implements Command {
    private Bot bot;

    public ExecCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "exec";
    }

    @Override
    public String getDescription() {
        return "executes a command on the server";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName() + " [command]", "executes a command on the server", false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(), getDescription())
                .addOption(OptionType.STRING, "command", "command to be executed", true);
    }

    @Override
    public void run(SlashCommandEvent event) {
        event.deferReply().queue();
        if (!bot.server.isRunning()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("MC Server:");
            embed.setDescription("Server Not Running");
            embed.setColor(Color.BLUE);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } else {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("MC Server:");
                embed.setDescription("Executing");
                embed.setColor(Color.DARK_GRAY);
                event.getHook().sendMessageEmbeds(embed.build()).queue();
            if (!bot.server.command(event.getOption("command").getAsString())) {
                embed = new EmbedBuilder();
                    embed.setTitle("MC Server:");
                    embed.setDescription("Error Executing command");
                    embed.setColor(Color.RED);
                    event.getHook().editOriginalEmbeds(embed.build()).queue();
            } else {
                    embed = new EmbedBuilder();
                    embed.setTitle("MC Server:");
                    embed.setDescription("Execution complete");
                    embed.setColor(Color.GREEN);
                    event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }
}
