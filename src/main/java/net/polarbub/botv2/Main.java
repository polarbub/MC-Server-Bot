package net.polarbub.botv2;

import me.dilley.MineStat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.*;

public class Main extends ListenerAdapter {
    public static ProcessBuilder pb;
    public static Process p;
    public static BufferedWriter bw;
    public static BufferedReader br;
    public static boolean serverRunning = false;
    public static JDA bot;
    public static in inThread = new in();
    public static out outThread = new out();
    public static server serverThread = new server();
    public static git gitThread = new git();
    public static status statusThread = new status();
    public static Pattern ipPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
    public static Pattern urlPattern = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}");


    public static void main(String[] args) throws IOException, InterruptedException, LoginException {
        config.readConfig();

        pb = new ProcessBuilder(serverArgs);
        pb.directory(new File(serverDir));
        pb.redirectErrorStream(true);

        //start the console in a thread
        inThread.start();

        //Start the console out
        outThread.start();

        //Start status thing
        statusThread.start();
    }

    //message processing
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            MessageChannel returnChannel = event.getChannel();
            System.out.println();
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
                    config.readConfig();
                } catch (IOException | InterruptedException | LoginException e) {
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
                MineStat ms = new MineStat(IP, port);
                String out = "Server is";
                if(ms.isServerUp()) {
                    out = String.join(" ", out, "up\n\n");
                    out = String.join("", out, String.valueOf(ms.getCurrentPlayers()), " out of ", String.valueOf(ms.getMaximumPlayers()), " players\n\n");
                    out = String.join("", out, "MOTD: `", String.valueOf(ms.getMotd()), "`\n\n");
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
                    git.runProg(new ProcessBuilder("git", "branch", msg.getContentRaw().substring(16) + "_rollback_" + DateTimeFormatter.ofPattern("yyyy/MM/dd_HH:mm:ss").format(LocalDateTime.now())), serverDir);
                    git.runProg(new ProcessBuilder("git", "reset", "--hard", msg.getContentRaw().substring(16)), serverDir);
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
                    /*String[] url;

                    Matcher matcher = urlPattern.matcher(msg.getContentRaw());
                    if(matcher.matches()) {

                    }*/

                    try {
                        Color c = event.getMember().getColor();
                        int R = c.getRed();
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
                            "\",\"color\":\"white\", \"clickEvent\":{\"action\":\"open_url\",\"value\":\"" +
                            //url +
                            "\"}]");
                }
            }
        }
    }
}