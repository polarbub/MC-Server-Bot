package net.polarbub.botv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import javax.security.auth.login.LoginException;
import java.io.*;
import com.amihaiemil.eoyaml.*;


public class Main extends ListenerAdapter {
    public static String pre;
    public static ProcessBuilder pb;
    public static TextChannel consoleChannel;
    public static MessageChannel returnChannel;
    public static Process p;
    public static BufferedWriter bw;
    public static BufferedReader br;
    public static boolean serverRunning = false;
    public static YamlMapping config;
    public static String serverArgs;
    public static YamlMapping discordConfig;
    public static String token;
    public static YamlMapping minecraftConfig;
    public static YamlMapping permissionsConfig;
    public static JDA bot;


    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        Main.configInit();
        //init discord jda
        bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000);

        consoleChannel = bot.getTextChannelById(discordConfig.string("CONSOLE_CHANNEL"));
        pb = new ProcessBuilder(serverArgs);
        pb.directory(new File("E:\\saves\\.minecraft\\server\\protosky-testing"));
        pb.redirectErrorStream(true);

        //start the console in a thread
        in.main();
        consoleChannel.sendMessageFormat("line one\nline 2").queue();

    }

    public static void configInit() throws IOException {
        config = Yaml.createYamlInput(new File("config.yaml")).readYamlMapping();
        discordConfig = config.yamlMapping("DISCORD_BOT");
        token = discordConfig.string("TOKEN");
        pre = discordConfig.string("PREFIX");
        minecraftConfig = config.yamlMapping("MC_SERVER");
        serverArgs = minecraftConfig.string("startCMD");
        permissionsConfig = config.yamlMapping("PERMISSIONS");

    }

    //message processing
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            returnChannel = event.getChannel();

            if (msg.getContentRaw().equals(pre + "init") && permissions.getPermissions("init", event)) {
                System.out.println("done");
                returnChannel.sendMessageFormat("done").queue();


            } else if (msg.getContentRaw().equals(pre + "stopbot") && permissions.getPermissions("stopbot", event)) {
                System.exit(0);

            } else if (msg.getContentRaw().equals(pre + "start") && String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && !serverRunning && permissions.getPermissions("server", event)) {
                if(serverRunning) {
                    returnChannel.sendMessageFormat("Server is Running rn").queue();
                } else {
                    serverRunning = true;
                    server.main();
                }

            } else if(msg.getContentRaw().equals(pre + "reloadconfig") && permissions.getPermissions("reloadconfig", event)) {
                try {
                    configInit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                returnChannel.sendMessageFormat("Done").queue();

            } else {
                if (in.tosay.equals(msg.getContentRaw())) {
                    int a = 0;
                } else {
                    System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                    System.out.println("Content: " + msg.getContentRaw());
                }

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && serverRunning && permissions.getPermissions("server", event)) {
                    try {
                        Main.bw.write(msg.getContentRaw());
                        Main.bw.newLine();
                        Main.bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}