package com.mattymatty.mcbot.discord;

import com.mattymatty.mcbot.discord.commands.Command;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Listener extends ListenerAdapter {
    Bot bot;
    public Listener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event)
    {
        if (event.getGuild() == null)
            return;

        if (!bot.canExecuteCommand(event.getTextChannel())) {
            event.reply("You're not allowed to do this").setEphemeral(true).queue();
            return;
        }
        Command cmd = bot.commandMap.get(event.getName());
        if(cmd!=null)
            cmd.run(event);
    }

    @Override
    public void onTextChannelUpdateTopic(@NotNull TextChannelUpdateTopicEvent event) {
        bot.channelManager.channel_edited(event);
    }

    @Override
    public void onTextChannelCreate(@NotNull TextChannelCreateEvent event) {
        bot.channelManager.channel_created(event);
    }

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        bot.channelManager.channel_deleted(event);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        bot.channelManager.reloadChannels();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        bot.channelManager.reloadChannels();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        bot.channelManager.message_received(event);
    }
}
