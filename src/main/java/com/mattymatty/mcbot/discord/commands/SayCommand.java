package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.discord.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.awt.*;

public class SayCommand implements Command{
    private Bot bot;

    public SayCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "say";
    }

    @Override
    public String getDescription() {
        return "sends a chat message to the server";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName() + " [msg]","sends a chat message to the server",false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(),getDescription())
                .addOption(OptionType.STRING,"msg","message to be displayed",true);
    }

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
            embed.setDescription("Sending");
            embed.setColor(Color.DARK_GRAY);
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            if(!bot.say(event.getMember(),event.getOption("msg").getAsString())){
                embed = new EmbedBuilder();
                embed.setTitle("MC Server:");
                embed.setDescription("Error Sending Mesage");
                embed.setColor(Color.RED);
                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }else{
                embed = new EmbedBuilder();
                embed.setTitle("MC Server:");
                embed.setDescription("Sent");
                embed.setColor(Color.GREEN);
                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }
}
