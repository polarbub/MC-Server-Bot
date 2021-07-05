package com.mattymatty.mcbot;

import com.mattymatty.mcbot.backup.GitWrapper;
import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import com.mattymatty.mcbot.terminal.Console;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static Log LOG;
    public static GitWrapper GIT;
    public static void main(String[] args) throws IOException {
        Yaml yaml = new Yaml(new Constructor(Config.class));
        Config cfg = yaml.load(new FileInputStream("config.yaml"));
        LOG = new Log();
        GIT = new GitWrapper(cfg);
        Server server = new Server(cfg);
        Bot bot = new Bot(cfg,server);
        bot.loadCommands().findChannels();

        Console console = new Console(server,bot,cfg);

        try{
            console.start();
        }catch (IOException ex){
            System.err.println("Could not create UI, fallback to println");
            server.addConsoleListener(System.out::println);
            server.addErrorListener(e->{
                Arrays.stream(e.split("\n")).skip(1).forEach(System.err::println);
            });
        }
    }
}
