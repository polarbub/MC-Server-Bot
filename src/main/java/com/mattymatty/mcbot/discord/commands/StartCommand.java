package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class StartCommand implements Command{
    private Bot bot;

    public StartCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "starts the server";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName()," starts the minecraft Server",false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(),getDescription());
    }

    Server.DataListener stop,start;

    @Override
    public void run(SlashCommandEvent event) {
        if (!bot.canExecuteCommand(event.getTextChannel())) {
            event.reply("You're not allowed to do this").setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue();
        if(bot.server.isRunning()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("MC Server:");
            embed.setDescription("Server Already Running");
            embed.setColor(Color.BLUE);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }else{
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("MC Server:");
            embed.setDescription("Starting the server");
            embed.setColor(Color.DARK_GRAY);
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            stop = s-> {
                EmbedBuilder embed2 = new EmbedBuilder();
                embed2.setTitle("MC Server:");
                embed2.setDescription("Server Failed to Start");
                embed2.setColor(Color.RED);
                event.getHook().editOriginalEmbeds(embed2.build()).queue();
                bot.server.removeStopListener(this.stop);
                bot.server.removeStopListener(this.start);
            };
            start = s->{
                EmbedBuilder embed2 = new EmbedBuilder();
                embed2.setTitle("MC Server:");
                embed2.setDescription("Server Started");
                embed2.setColor(Color.GREEN);
                event.getHook().editOriginalEmbeds(embed2.build()).queue();
                bot.server.removeStopListener(this.stop);
                bot.server.removeStopListener(this.start);
            };
            bot.server.addStartListener(start);
            bot.server.addStopListener(stop);
            if(!bot.server.start())
                this.stop.listen("");
        }
    }
}
