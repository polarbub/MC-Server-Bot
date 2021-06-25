package com.mattymatty.mcbot;

import com.mattymatty.mcbot.discord.Bot;
import com.mattymatty.mcbot.minecraft.Server;
import com.mattymatty.mcbot.terminal.Console;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Yaml yaml = new Yaml(new Constructor(Config.class));
        Config cfg = yaml.load(new FileInputStream("config.yaml"));
        Server server = new Server(cfg);
        Bot bot = new Bot(cfg,server);
        bot.loadCommands().findChannels();

        Console console = new Console(server,bot,cfg);

        server.addConsoleListener(System.out::println);

        console.start();
    }
}
