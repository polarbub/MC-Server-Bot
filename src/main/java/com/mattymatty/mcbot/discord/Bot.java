package com.mattymatty.mcbot.discord;


import com.mattymatty.mcbot.Config;
import com.mattymatty.mcbot.discord.commands.*;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {
    private final JDA instance;
    private final Config config;
    public final List<Guild> guilds = new LinkedList<>();
    public final Map<String,Command> commandMap = new HashMap<>();
    public final Server server;
    public final ChannelManager channelManager;

    public Bot(Config config,Server server){
        try {
            this.config = config;
            this.server = server;
            instance = JDABuilder.createLight(config.DISCORD_BOT.token).build().awaitReady();
            channelManager = new ChannelManager(instance,server,config,this);
            instance.addEventListener(new Listener(this));
            loadGuilds();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void loadGuilds(){
        guilds.addAll(Arrays.stream(config.DISCORD_BOT.guilds).map(instance::getGuildById).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public Bot loadCommands(){
        instance.updateCommands().queue();
        Command cmd = new HelpCommand(this);
        commandMap.put(cmd.getName(),cmd);
        cmd = new ExecCommand(this);
        commandMap.put(cmd.getName(),cmd);
        cmd = new SayCommand(this);
        commandMap.put(cmd.getName(),cmd);
        cmd = new StartCommand(this);
        commandMap.put(cmd.getName(),cmd);
        cmd = new StopCommand(this);
        commandMap.put(cmd.getName(),cmd);

        guilds.forEach(g->{
            CommandListUpdateAction commands = g.updateCommands();
            commandMap.values().forEach(c->commands.addCommands(c.getCommand()));
            commands.queue();
        });

        return this;
    }

    public boolean canExecuteCommand(TextChannel channel){
        return (!channelManager.isChatChannel(channel) &&
                (config.DISCORD_BOT.command_marker_mode.equals("ALLOW"))) == (channelManager.isCommandChannel(channel));

    }

    public Bot findChannels(){
        channelManager.loadChannels();
        return this;
    }

    public boolean say(Message message){
        return SayHandler.say(server,config,message);
    }
    public boolean say(Member member, String message){
        return SayHandler.say(server,config,member,message,null,null);
    }
    

}
