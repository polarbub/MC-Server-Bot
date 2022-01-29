package net.polarbub.botv2;

import com.vdurmont.emoji.EmojiParser;

import me.dilley.MineStat;
import net.dv8tion.jda.api.EmbedBuilder;
import rmmccann.Minecraft.*;

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

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
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
            if(stopHard || (server.serverRunning && !server.serverStarted)) {
                server.p.destroy();
                return;
            } else if(server.serverStarted) {
                stopHard = true;
                System.out.println("\nWarning, killing running server!\nSend SIGINT again to hard stop the server.\nStrangely this doesn't print the normal server stopping lines even though it is stopping it cleanly.\nThe process with exit when the server is stopped.");
                //Fix: This doesn't print out the normal stopping text. The BR doesn't have anything in it. This is a won't fix.
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
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (configLoadedFailure) {
            bot.shutdown();
            System.exit(1);
        } else if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            MessageChannel returnChannel = event.getChannel();
            if (msg.getContentRaw().equals("start") && String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && permissions.getPermissions("server", event)) {
                if(server.serverRunning) {
                    returnChannel.sendMessageFormat("Server is Running.").queue();
                } else {
                    Main.serverThread = new server();
                    Main.serverThread.start();
                }
            } else if(msg.getContentRaw().equals(pre + "help")) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Help");
                embedBuilder.addField("Server Command", "Send a message in <#" + consoleChannel.getId() + ">", false);
                embedBuilder.addField("Start Server", "Send `start` in <#" + consoleChannel.getId() + ">", false);
                embedBuilder.addField("Terminal", "Both these things can also be done in the terminal", false);
                embedBuilder.addField("Chat Bridge", "Send a message in <#" + chatBridgeChannel.getId() + ">", false);
                embedBuilder.addBlankField(false);
                if(backupTime != 0) {
                    embedBuilder.addField(pre + "backup <backup message>", "Backup the server", false);
                    embedBuilder.addField(pre + "backup restore <commit id>", "Restore backup", false);
                    embedBuilder.addField(pre + "backup pause <amount>", "Stop backing up for <amount> backups. Set amount to 0 to reset counter", false);
                    embedBuilder.addField(pre + "backup pause get", "Print the amount of backups to be skipped", false);
                }
                embedBuilder.addField(pre + "help", "print this message", false);
                returnChannel.sendMessageEmbeds(embedBuilder.build()).queue();

            } else if (msg.getContentRaw().equals(pre + "status") && permissions.getPermissions("status", event)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Minecraft Server", null);
                System.out.println(server.serverStarted);
                if(server.serverStarted) {
                    System.out.println(realIP + "in" + pingPort);
                    MineStat ms = new MineStat(realIP, pingPort);
                    if(ms.isServerUp()) {
                        System.out.println("in ms");
                        try {
                            QueryResponse fullTest = new MCQuery(realIP, queryPort).fullStat();
                            embedBuilder.setColor(Color.green);
                            embedBuilder.setDescription("Server is up");
                            if(!showIP.isEmpty()) {
                                embedBuilder.addField("IP", showIP, true);
                                embedBuilder.addField("Port", String.valueOf(pingPort), true);
                                embedBuilder.addField("Version", ms.getVersion(), true);
                            } else {
                                embedBuilder.addField("Version", ms.getVersion(), false);
                            }
                            embedBuilder.addField("MOTD", ms.getMotd(), false);
                            embedBuilder.addField("Players", ms.getCurrentPlayers() + " out of " + ms.getMaximumPlayers() + " max", false);
                            if (ms.getCurrentPlayers() > 0) {
                                embedBuilder.addField("Player List", fullTest.getPlayerList().toString().replaceAll("(?:(?:\\[)|(?:\\]))", ""), false);
                            }
                            System.out.println("embed");
                        } catch (RejectedExecutionException e) {
                            embedBuilder.setColor(Color.red);
                            embedBuilder.setDescription("The server could not be contacted over the query protocol");
                            System.out.println("The server could not be contacted over the query protocol. Please make sure that the port of the query protocol matches the one set in the config");
                        }
                    }

                } else {
                    embedBuilder.setColor(Color.red);
                    embedBuilder.setDescription("Server is off");
                }
                returnChannel.sendMessageEmbeds(embedBuilder.build()).queue();

            } else if(msg.getContentRaw().startsWith(pre + "backup") && backupTime != 0 && permissions.getPermissions("backup", event)) {

                if(msg.getContentRaw().startsWith(pre + "backup restore")) {
                    if(!server.serverRunning && msg.getContentRaw().length() >= 15 + pre.length() && msg.getContentRaw().length() <= 22) {
                        git.rollBack(msg.getContentRaw().substring(15 + pre.length()));
                    } else if(server.serverRunning) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setDescription("Please stop the server first").build()).queue();
                    } else {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setDescription("Please specify a commit id to roll back to. It should be 7 digits of base 62").build()).queue();
                    }
                } else if(msg.getContentRaw().startsWith(pre + "backup pause ")) {
                    if(msg.getContentRaw().startsWith(pre + "backup pause get")) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setDescription(git.backupPauseAmount + " Skipped backups left").build()).queue();
                    } else {
                        try {
                            int check = Integer.parseInt(msg.getContentRaw().substring(13 + pre.length()));
                            if (check < 0) {
                                returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setDescription("Negative Integer. Please provide a non-negative integer.").build()).queue();
                            } else {
                                git.backupPauseAmount = check;
                            }
                        } catch (NumberFormatException e) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setDescription("Invalid Integer. Please type a valid integer.").build()).queue();
                        }
                    }
                } else if(msg.getContentRaw().startsWith(pre + "backup")) {
                    if(msg.getContentRaw().length() <= 7 + pre.length()) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setDescription("Please specify a commit comment").build()).queue();
                    } else {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setDescription(git.backup(msg.getContentRaw().substring(7 + pre.length()))).build()).queue();
                    }
                }

            } else {
                //System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                //System.out.println("Content: " + msg.getContentDisplay());

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && permissions.getPermissions("server", event)) {
                    server.commandUse(msg.getContentRaw());
                } else if (String.valueOf(returnChannel).equals(String.valueOf(chatBridgeChannel)) && permissions.getPermissions("chatbridge", event)) {
                    JSONArray command = new JSONArray();

                    command.put(new JSONObject()
                            .put("color", "#7289DA")
                            .put("text", "[DISCORD]")
                    );

                    command.put(new JSONObject()
                            .put("color", "white")
                            .put("text", " <")
                    );

                    String rgb = "";

                    try {
                        Color c = event.getMember().getColor();
                        int R = c.getRed();
                        int G = c.getGreen();
                        int B = c.getBlue();

                        rgb =  "#" + Integer.toHexString(R) + Integer.toHexString(G) + Integer.toHexString(B);
                    } catch (NullPointerException ignored) {
                        rgb = "#FFFFFF";
                    }

                    command.put(new JSONObject()
                            .put("text", msg.getAuthor().getName())
                            .put("color", rgb)
                    );

                    command.put(new JSONObject()
                            .put("color", "white")
                            .put("text", "> ")
                    );
                    
                    Message replyMessage = msg.getReferencedMessage();

                    if(replyMessage != null) {
                        command.put(new JSONObject()
                                .put("color", "white")
                                .put("text", "in reply to ")
                        );

                        String rgbReply = "";
                        try {
                            Color c = event.getGuild().retrieveMember(replyMessage.getAuthor()).complete().getColor();
                            int R = c.getRed();
                            int G = c.getGreen();
                            int B = c.getBlue();

                            rgbReply =  "#" + Integer.toHexString(R) + Integer.toHexString(G) + Integer.toHexString(B);
                        } catch (NullPointerException e) {
                            rgbReply = "#FFFFFF";
                        }

                        command.put(new JSONObject()
                                .put("clickEvent", new JSONObject()
                                        .put("action", "open_url")
                                        .put("value", replyMessage.getJumpUrl())
                                )
                                .put("hoverEvent", new JSONObject()
                                        .put("action", "show_text")
                                        .put("value", "Click to open the message")
                                )
                                .put("underlined" , "true")
                                .put("color", rgbReply)
                                .put("text", replyMessage.getAuthor().getName())

                        );

                        command.put(new JSONObject()
                                .put("color", "white")
                                .put("text", ": ")
                        );
                    }

                    String msgContent = msg.getContentDisplay();
                    msgContent = EmojiParser.parseToAliases(msgContent, EmojiParser.FitzpatrickAction.PARSE);

                    Pattern pattern = Pattern.compile("https*://[!-~]+");
                    Matcher matcher = pattern.matcher(msg.getContentDisplay());

                    if(matcher.find()) {
                        matcher.reset();
                        int lastend = 0;
                        while(matcher.find()) {
                            command.put(new JSONObject()
                                    .put("color", "white")
                                    .put("text", msgContent.substring(lastend, matcher.start()))
                            );
                            command.put(new JSONObject()
                                    .put("clickEvent", new JSONObject()
                                            .put("action", "open_url")
                                            .put("value", matcher.group())
                                    )
                                    .put("hoverEvent", new JSONObject()
                                            .put("action", "show_text")
                                            .put("value", "Click to open link")
                                    )
                                    .put("underlined" , "true")
                                    .put("color", "blue")
                                    .put("text", msgContent.substring(matcher.start(), matcher.end()))
                            );
                            lastend = matcher.end();
                        }
                    } else {
                        command.put(new JSONObject()
                                .put("color", "white")
                                .put("text", msgContent)
                        );
                    }

                    if(!msg.getAttachments().isEmpty()) {
                        if(msg.getAttachments().size() <= 1) command.put(new JSONObject()
                                .put("color", "gray")
                                .put("text", " | Attachment: ")
                        ); else if(msg.getContentRaw().length() == 0); else command.put(new JSONObject()
                                .put("color", "gray")
                                .put("text", " | Attachments: ")
                        );
                        for(Message.Attachment attachment : msg.getAttachments()) {
                            command.put(new JSONObject()
                                    .put("clickEvent", new JSONObject()
                                            .put("action", "open_url")
                                            .put("value", attachment.getUrl())
                                    )
                                    .put("hoverEvent", new JSONObject()
                                            .put("action", "show_text")
                                            .put("value", "Click to open attachment")
                                    )
                                    .put("underlined" , "true")
                                    .put("color", "blue")
                                    .put("text", attachment.getFileName())
                            );
                            command.put(new JSONObject()
                                    .put("color", "white")
                                    .put("text", ", ")
                            );
                        }
                        command.remove(command.length() - 1);
                    }

                    server.commandUse("/execute if entity @a run tellraw @a " + command.toString());
                }
            }
        }
    }
}