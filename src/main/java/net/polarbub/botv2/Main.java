package net.polarbub.botv2;

import me.dilley.MineStat;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.message.in;
import net.polarbub.botv2.message.out;
import net.polarbub.botv2.server.git;
import net.polarbub.botv2.server.server;

import static net.polarbub.botv2.config.config.*;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.regex.Pattern;

public class Main extends ListenerAdapter {
    public static String[] runTimeArgs;
    public static in inThread = new in();
    public static out outThread = new out();
    public static server serverThread;
    public static git gitThread = new git();
    public static status statusThread = new status();
    public static boolean configLoadedFailure = false;
    public static boolean stopHard = false;
    public static Pattern ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    public static void main(String[] args) throws InterruptedException, LoginException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(stopHard && server.serverRunning) {
                server.p.destroy();
                return;
            } else if(server.serverRunning) {
                stopHard = true;
                System.out.println("\nWarning, killing running server!\nSend SIGINT again to hard stop the server.\nStrangely this doesn't print the normal server stopping lines even though it is stopping it cleanly.\nThe process with exit when the server is stopped.");
                //FIX: This doesn't print out the normal stopping text
                server.commandUse("stop");
                while(server.serverRunning) { //wait for server off
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            bot.shutdown();
        }));

        runTimeArgs = args;
        try {
            config.readConfig();
        } catch (IOException e) {
            System.out.println("Invalid config file location");
            configLoadedFailure = true;
            throw e;
        } catch (NullPointerException e) {
            System.out.println("Invalid config file");
            configLoadedFailure = true;
            throw e;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No config file declared, please append config file location to command used to start bot");
            configLoadedFailure = true;
            throw e;
        }

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
        if (configLoadedFailure) {
            bot.shutdown();
            System.exit(1);
        } else if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            MessageChannel returnChannel = event.getChannel();
            if (msg.getContentRaw().equals("start") && String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && !server.serverRunning && permissions.getPermissions("server", event)) {
                if(server.serverRunning) {
                    returnChannel.sendMessageFormat("Server is Running.").queue();
                } else {
                    Main.serverThread = new server();
                    Main.serverThread.start();
                }
            } else if(msg.getContentRaw().equals(pre + "help")) {
                returnChannel.sendMessageFormat(
                        "```\n" +  pre + "status                     | get the status of the server\n" +
                        pre + "backup <backup message>    | backup the server\n" +
                        pre + "backup restore <commit id> | restore backup```" +
                        "\nTo send a command to the server send a message in <#" + consoleChannel.getId() + ">. " +
                        "To start the server send `start` in <#" + consoleChannel.getId() + ">. Both these things can also be done in the terminal.\n" +
                        "To send a message through the chat bridge send a message in <#" + chatBridgeChannel.getId() + ">.\n"
                ).queue();

            } else if (msg.getContentRaw().equals(pre + "status") && permissions.getPermissions("status", event)) {
                String out = "Server is";
                if(server.serverStarted) {
                    MineStat ms = new MineStat(IP, port);
                    if(ms.isServerUp()) {
                        out = String.join(" ", out, "up\n\n");
                        out = String.join("", out, String.valueOf(ms.getCurrentPlayers()), " out of ", String.valueOf(ms.getMaximumPlayers()), " players\n\n");
                        out = String.join("", out, "MOTD: `", String.valueOf(ms.getMotd()), "`\n\n");
                    } else {
                        out = String.join(" ", out, "down\n\n");
                    }
                    returnChannel.sendMessageFormat(out).queue();
                } else {
                    returnChannel.sendMessageFormat(out + " off").queue();
                }

            } else if(msg.getContentRaw().startsWith(pre + "backup restore") && permissions.getPermissions("backup", event)) {
                if(!server.serverRunning && msg.getContentRaw().length() >= 16) {
                    git.rollBack(msg.getContentRaw().substring(16));
                } else if(server.serverRunning) {
                    returnChannel.sendMessageFormat("Please stop the server first").queue();
                } else {
                    returnChannel.sendMessageFormat("Please specify a commit id to roll back to. It should be 7 digits of base 62").queue();
                }
            } else if(msg.getContentRaw().startsWith(pre + "backup") && permissions.getPermissions("backup", event)) {
                if(msg.getContentRaw().length() <= 8) {
                    returnChannel.sendMessageFormat("Please specify a commit comment").queue();
                } else {
                    returnChannel.sendMessageFormat(git.backup(msg.getContentRaw().substring(8))).queue();
                }

            } else {
                System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                System.out.println("Content: " + msg.getContentRaw());

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && server.serverRunning && permissions.getPermissions("server", event)) {
                    server.commandUse(msg.getContentRaw());
                } else if (String.valueOf(returnChannel).equals(String.valueOf(chatBridgeChannel)) && server.serverRunning && permissions.getPermissions("chatbridge", event)) {
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

                    /*ADD:
                        - Attachments
                        - Use json builder for discord chat bridge -> mc
                        - Replys
                        - URLS
                        - emoji lookup with https://github.com/vdurmont/emoji-java*/

                    server.commandUse(

                            "/tellraw @a [{\"text\":\"[DISCORD]\",\"color\":\"#7289DA\"},{\"text\":\" <\",\"color\":\"white\"},{\"text\":\"" +
                                    msg.getAuthor().getName() +
                                    "\",\"color\":\"" +
                            rgb +
                            "\"},{\"text\":\"> \",\"color\":\"white\"},{\"text\":\"" +
                                    msg.getContentRaw() +
                            "\",\"color\":\"white\"" +

                                    //", \"clickEvent\":{\"action\":\"open_url\",\"value\":\"" +
                            //url +
                            "}]");
                }
            }
        }
    }
}