package com.mattymatty.mcbot.discord.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface Command {
    String getName();

    String getDescription();

    MessageEmbed getHelp();

    CommandData getCommand();

    void run(SlashCommandEvent event);
}
