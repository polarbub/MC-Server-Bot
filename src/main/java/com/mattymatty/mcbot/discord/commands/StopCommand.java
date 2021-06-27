package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;

public class StopCommand implements Command{
    private Bot bot;

    public StopCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "stops the server";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName()," stops the minecraft Server",false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(),getDescription());
    }

    Server.DataListener stop,start;

    @Override
    public void run(SlashCommandEvent event) {
        event.deferReply().queue();
        if(!bot.server.isRunning()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("MC Server:");
            embed.setDescription("Server Not Running");
            embed.setColor(Color.BLUE);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }else{
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("MC Server:");
            embed.setDescription("Stopping the server");
            embed.setColor(Color.DARK_GRAY);
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            stop = s-> {
                EmbedBuilder embed2 = new EmbedBuilder();
                embed2.setTitle("MC Server:");
                embed2.setDescription("Server Stopped");
                embed2.setColor(Color.BLUE);
                event.getHook().editOriginalEmbeds(embed2.build()).queue();
                bot.server.removeStopListener(this.stop);
            };
            bot.server.addStopListener(stop);
            if(!bot.server.stop()) {
                EmbedBuilder embed2 = new EmbedBuilder();
                embed2.setTitle("MC Server:");
                embed2.setDescription("Server Failed to Stop");
                embed2.setColor(Color.RED);
                event.getHook().editOriginalEmbeds(embed2.build()).queue();
            }
        }
    }
}
