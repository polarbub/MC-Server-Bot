package net.polarbub.botv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.*;
import com.amihaiemil.eoyaml.*;
import me.dilley.MineStat;

public class Main extends ListenerAdapter {
    public static String pre;
    public static ProcessBuilder pb;
    public static TextChannel consoleChannel;
    public static MessageChannel returnChannel;
    public static Process p;
    public static BufferedWriter bw;
    public static BufferedReader br;
    public static boolean serverRunning = false;
    public static String serverArgs;
    public static String token;
    public static JDA bot;
    public static long backupTime;
    public static long backupWarn;
    public static in inThread = new in();
    public static out outThread = new out();
    public static server serverThread = new server();
    public static git gitThread = new git();
    public static status statusThread = new status();
    public static TextChannel chatBridgeChannel;
    public static int port;
    public static String showIP;
    public static String trueIP;
    public static Pattern[] pattern;
    public static YamlMapping discordConfig;
    public static YamlMapping permissionsConfig;
    public static String tellCommand;



    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        Main.configInit();

        //init discord jda
        bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000);

        consoleChannel = bot.getTextChannelById(discordConfig.longNumber("CONSOLE_CHANNEL"));
        chatBridgeChannel = bot.getTextChannelById(discordConfig.longNumber("CHAT_CHANNEL"));

        //Get the channel IDs
        pb = new ProcessBuilder(serverArgs);
        pb.directory(new File("server\\"));
        pb.redirectErrorStream(true);

        //start the console in a thread
        inThread.start();

        //Start the console out
        outThread.start();

        //Start status thing
        statusThread.start();
    }

    //Read the config file
    public static void configInit() throws IOException {
        YamlMapping config = Yaml.createYamlInput(new File("config.yaml")).readYamlMapping();
        YamlMapping backupConfig = config.yamlMapping("BACKUP");
        discordConfig = config.yamlMapping("DISCORD_BOT");
        YamlMapping minecraftConfig = config.yamlMapping("MC_SERVER");
        permissionsConfig = config.yamlMapping("PERMISSIONS");

        serverArgs = minecraftConfig.string("startCMD");
        port = minecraftConfig.integer("port");
        showIP = minecraftConfig.string("status_ip");
        trueIP = minecraftConfig.string("ip");
        tellCommand = minecraftConfig.string("say_command");

        YamlSequence chatBridgeRegexSeq = minecraftConfig.yamlSequence("chat_regex");
        pattern = new Pattern[chatBridgeRegexSeq.size()];
        for(int i = 0; i < chatBridgeRegexSeq.size(); i++) {
            pattern[i] = Pattern.compile(chatBridgeRegexSeq.string(i));
        }

        token = discordConfig.string("TOKEN");
        pre = discordConfig.string("PREFIX");

        backupTime = backupConfig.longNumber("backup_time");
        backupWarn = backupConfig.longNumber("backup_alert");
    }

    //message processing
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            returnChannel = event.getChannel();
            if (msg.getContentRaw().equals(pre + "stopbot") && permissions.getPermissions("stopbot", event)) {
                if(Main.serverRunning) {
                    out.add("Server is Running rn");
                } else {
                    System.exit(0);
                }

            } else if (msg.getContentRaw().equals("start") && String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && !serverRunning && permissions.getPermissions("server", event)) {
                if(serverRunning) {
                    out.add("Server is Running rn");
                } else {
                    serverThread.start();
                }

            } else if(msg.getContentRaw().equals(pre + "reloadconfig") && permissions.getPermissions("reloadconfig", event)) {
                try {
                    configInit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                returnChannel.sendMessageFormat("Done").queue();

            } else if(msg.getContentRaw().equals(pre + "help")) {
                returnChannel.sendMessageFormat("**HELP**\n\n" +
                        "Always add you prefix before the command. Yours is: `" + pre + "` " +
                        "```\nstopbot                   | Stop server bot. This can also be done in the terminal\n" +
                        "reloadconfig              | reload the configuration from config.yaml\n" +
                        "status                    | get the status of the server\n" +
                        "backup <backup message>   | back up the server```" +
                        "backup restore <commit id | restore backup```" +
                        "\nTo send a command to the server send a message in your console channel. This can also be done in the terminal.\n" +
                        "To send a message through the chat bridge send a message in your chat bridge channel.\n" +
                        "To start the server send `start` in the console channel. This can also be done in the terminal."
                ).queue();

            } else if (msg.getContentRaw().equals(pre + "status") && permissions.getPermissions("status", event) && Main.serverRunning) {
                MineStat ms = new MineStat(trueIP, port);
                int port2 = 26656;
                MineStat ms2b = new MineStat("2b2t.org", port2);
                String out = "Server is";
                if(ms.isServerUp()) {
                    out = String.join(" ", out, "up\n\n");
                    out = String.join("", out, String.valueOf(ms.getCurrentPlayers()), " out of ", String.valueOf(ms.getMaximumPlayers()), " players\n\n");
                    out = String.join("", out, "MOTD: `", String.valueOf(ms.getMotd()), "`\n\n");
                    if(ms2b.isServerUp()) out = String.join("", out, "2b2t MOTD: `", String.valueOf(ms2b.getMotd()), "`\n\n");
                    out = String.join("", out, "`", trueIP, ":", String.valueOf(port), "`\n\n");
                } else {
                    out = String.join(" ", out, "down\n\n");
                }
                returnChannel.sendMessageFormat(out).queue();

            } else if(msg.getContentRaw().startsWith(pre + "backup restore") && permissions.getPermissions("backup", event)) {
                if(!Main.serverRunning) {
                    git.backup("before rollback");
                    while (git.gitInUse) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    git.gitInUse = true;
                    git.runProg(new ProcessBuilder("git", "reset", "--hard", msg.getContentRaw().substring(16)));
                    git.runProg(new ProcessBuilder("git", "branch", msg.getContentRaw().substring(16) + "_rollback_" + DateTimeFormatter.ofPattern("yyyy/MM/dd_HH:mm:ss").format(LocalDateTime.now())));
                    git.gitInUse = false;
                } else {
                    out.add("Please stop the server first");
                }

            } else if(msg.getContentRaw().startsWith(pre + "backup") && permissions.getPermissions("backup", event)) {
                git.backup(msg.getContentRaw().substring(8));

            } else {
                System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                System.out.println("Content: " + msg.getContentRaw());

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && serverRunning && permissions.getPermissions("server", event)) {
                    server.commandUse(msg.getContentRaw());
                } else if (String.valueOf(returnChannel).equals(String.valueOf(chatBridgeChannel)) && serverRunning && permissions.getPermissions("chatbridge", event)) {
                    String rgb = "";
                    try {
                        Color c = event.getMember().getColor();
                        int R = 0;
                        try {
                            R = c.getRed();
                        } catch (NullPointerException ignored) {}
                        int G = c.getGreen();
                        int B = c.getBlue();
                        rgb =  "#" + Integer.toHexString(R) + Integer.toHexString(G) + Integer.toHexString(B);
                    } catch (NullPointerException ignored) {
                        rgb = "#FFFFFF";
                    }
                    server.commandUse(

                            "/tellraw @a [{\"text\":\"[DISCORD]\",\"color\":\"#7289DA\"},{\"text\":\" <\",\"color\":\"white\"},{\"text\":\"" +
                                    msg.getAuthor().getName() +
                                    "\",\"color\":\"" +
                            rgb +
                            "\"},{\"text\":\"> \",\"color\":\"white\"},{\"text\":\"" +
                                    msg.getContentRaw() +
                            "\",\"color\":\"white\"}]"
                                    );
                }
            }
        }
    }
}