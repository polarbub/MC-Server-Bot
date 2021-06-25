package com.mattymatty.mcbot.discord;

import com.mattymatty.mcbot.Config;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChannelManager {
    private final JDA api;
    private final Server server;
    private final Config config;
    private final Bot bot;
    private final Set<TextChannel> chat_channels = new HashSet<>();
    private final Set<TextChannel> console_channels = new HashSet<>();
    private final Set<TextChannel> command_channels = new HashSet<>();

    public ChannelManager(JDA api, Server server, Config config,Bot bot) {
        this.api = api;
        this.server = server;
        this.config = config;
        this.bot = bot;
        new Thread(this::chat_channel_thread,"Chat channels").start();
        new Thread(this::console_channel_thread,"Console channels").start();
        server.addConsoleListener(console_buffer::append);
        server.addChatListener(chat_buffer::append);
    }

    public void reloadChannels(){
        chat_channels.clear();
        console_channels.clear();
        command_channels.clear();
        loadChannels();
    }

    public void loadChannels(){
        bot.guilds.forEach(g->{
            g.getTextChannels().forEach(c->{
                if(c.getTopic()==null)
                    return;
                if(c.getTopic().contains(config.DISCORD_BOT.chat_channel_marker))
                    chat_channels.add(c);
                else {
                    if (c.getTopic().contains(config.DISCORD_BOT.console_channel_marker))
                        console_channels.add(c);
                    if (c.getTopic().contains(config.DISCORD_BOT.command_channel_marker))
                        command_channels.add(c);
                }
            });
        });
    }

    public void channel_edited(TextChannelUpdateTopicEvent event){
        if(bot.guilds.contains(event.getGuild()))
            channel(event.getChannel());
    }
    public void channel_created(TextChannelCreateEvent event){
        if(bot.guilds.contains(event.getGuild()))
            channel(event.getChannel());
    }
    public void channel_deleted(TextChannelDeleteEvent event){
        if(bot.guilds.contains(event.getGuild()))
            channel(event.getChannel());
    }
    public void message_received(GuildMessageReceivedEvent event){
        if(isChatChannel(event.getChannel()) && !event.getAuthor().isBot()){
            bot.say(event.getMessage());
        }
    }


    public boolean isChatChannel(TextChannel channel){
        return chat_channels.contains(channel);
    }
    public boolean isConsoleChannel(TextChannel channel){
        return console_channels.contains(channel);
    }
    public boolean isCommandChannel(TextChannel channel){
        return command_channels.contains(channel);
    }

    private void channel(TextChannel channel){
        if(channel.getTopic()==null)
            return;
        boolean chat_channel = channel.getTopic().contains(config.DISCORD_BOT.chat_channel_marker);
        boolean console_channel = !chat_channel && channel.getTopic().contains(config.DISCORD_BOT.console_channel_marker) ;
        boolean command_channel = !chat_channel && channel.getTopic().contains(config.DISCORD_BOT.command_channel_marker) ;
        if(chat_channel)
            chat_channels.add(channel);
        else
            chat_channels.remove(channel);
        if(console_channel)
            console_channels.add(channel);
        else
            console_channels.remove(channel);
        if(command_channel)
            command_channels.add(channel);
        else
            command_channels.remove(channel);
    }

    final StringBufferPlus chat_buffer = new StringBufferPlus();
    private void chat_channel_thread(){
        try {
            while(!Thread.interrupted()){
                if(chat_buffer.length()>0){
                    String to_print = chat_buffer.getAndReset();
                    List<String> messages = splitMessage(to_print);
                    for (TextChannel channel: chat_channels) {
                        for (String msg : messages) {
                            channel.sendMessage(msg).queue();
                        }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(250);
            }
        }catch (InterruptedException ignored) {}
    }

    final StringBufferPlus console_buffer = new StringBufferPlus();
    private void console_channel_thread(){
        try {
            while(!Thread.interrupted()){
                if(console_buffer.length()>0){
                    String to_print = console_buffer.getAndReset();
                    List<String> messages = splitMessage(to_print);
                    for (TextChannel channel: console_channels) {
                        for (String msg : messages) {
                            channel.sendMessage(msg).queue();
                        }
                    }
                }
                TimeUnit.SECONDS.sleep(1);
            }
        }catch (InterruptedException ignored) {}
    }


    private List<String> splitMessage(String msg)
    {
        String[] splitters ={"\n"," ",".",",",":"} ;
        LinkedList<String> result = new LinkedList<>();

        String sub = msg.substring(0,Math.min(2000,msg.length()));
        if(sub.length() == 2000){
            int index = -1;
            for(String separator : splitters){
                index = sub.lastIndexOf(separator);
                if(index!=-1)
                    break;
            }
            sub = msg.substring(0,index);
            String next = msg.substring(index+1);
            result.add(sub);
            result.addAll(this.splitMessage(next));
        }else{
            result.add(msg);
        }
        return result;
    }

    private static class StringBufferPlus {
        StringBuffer buffer= new StringBuffer();

        public synchronized StringBufferPlus append(String s){
            buffer.append(s);
            buffer.append("\r\n");
            return this;
        }

        public synchronized int length(){
            return buffer.length();
        }

        public synchronized String getAndReset(){
            String s = buffer.toString();
            buffer.setLength(0);
            return s;
        }

    }

}
