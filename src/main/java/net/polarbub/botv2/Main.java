package net.polarbub.botv2;

import com.vdurmont.emoji.EmojiParser;

import me.dilley.MineStat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import rmmccann.Minecraft.*;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.User;

import net.polarbub.botv2.config.config;
import net.polarbub.botv2.message.in;
import net.polarbub.botv2.message.out;
import net.polarbub.botv2.server.git;
import net.polarbub.botv2.server.server;
import static net.polarbub.botv2.config.config.*;
import static net.polarbub.botv2.server.runProg.runProgString;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends ListenerAdapter {
    public static String[] runTimeArgs;
    public static in inThread = new in();
    public static out outThread = new out();
    public static server serverThread = new server();
    public static git gitThread = new git();
    public static status statusThread = new status();
    public static boolean configLoadedFailure = false;
    public static boolean stopHard = false;
    public static Color Green = new Color(44,203,115);
    public static Pattern ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static void main(String[] args) throws InterruptedException, LoginException, IOException {
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
        } catch (ErrorResponseException e) {
            System.out.println("Could not connect to discord. Check your network");
            configLoadedFailure = true;
            System.exit(1);
        }

        if(backupTime > 0) {
            String statusText = runProgString(new ProcessBuilder("git", "status"));

            if(statusText.startsWith("fatal: ")) {
                boolean printed = false;

                if (statusText.startsWith("fatal: unsafe repository ('" +
                        serverDir +
                        "' is owned by someone else)\n" +
                        "To add an exception for this directory, call:")) {

                    System.out.println(statusText);
                    printed = true;

                } else if (statusText.startsWith("fatal: not a git repository")) {
                    System.out.println("fatal: '" + serverDir + "' is not a git initialized repo\n" +
                            "To make a git repo there call, call:\n" +
                            "\n" +
                            "        git init " + serverDir + "\n");
                    printed = true;
                }

                if(!printed) {
                    System.out.println(statusText);
                }
                bot.shutdown();
                System.exit(1);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //FIX: The second one can be true when the server process is stopped and null, but the after stop backup is still running.
            // This causes a null pointer exception trying to stop an already stopped process.
            if(stopHard || (Main.serverThread.serverRunning && !Main.serverThread.serverStarted)) {
                Main.serverThread.p.destroy();
                return;
            } else if(Main.serverThread.serverStarted) {
                stopHard = true;
                System.out.println("\nWarning, killing running server!\nSend SIGINT again to hard stop the server.\nStrangely this doesn't print the normal server stopping lines even though it is stopping it cleanly.\nThe process with exit when the server is stopped.");
                //FIX: This doesn't print out the normal stopping text. The BR doesn't have anything in it. This is a won't fix.
                Main.serverThread.commandUse("stop");
                while(Main.serverThread.serverRunning) { //wait for server off
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            bot.shutdown();
        }));

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
                if(Main.serverThread.serverRunning) {
                    returnChannel.sendMessageFormat("Server is Running.").queue();
                } else {
                    Main.serverThread = new server();
                    Main.serverThread.start();
                }
            } else if(msg.getContentRaw().equals(pre + "help") && permissions.getPermissions("help", event)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Help");
                embedBuilder.addField("Server Command", "Send a message in <#" + consoleChannel.getId() + ">", false);
                embedBuilder.addField("Start Server", "Send `start` in <#" + consoleChannel.getId() + ">", false);
                embedBuilder.addField("Terminal", "Both these things can also be done in the terminal", false);
                embedBuilder.addField("Chat Bridge", "Send a message in <#" + chatBridgeChannel.getId() + ">", false);
                embedBuilder.addBlankField(false);
                if(backupTime != 0) {
                    embedBuilder.addField(pre + "backup save <backup message>", "Backup the server", false);
                    embedBuilder.addField(pre + "backup restore <commit id>", "Restore backup", false);
                    embedBuilder.addField(pre + "backup pause <amount>", "Stop backing up for `amount` backups. Set `amount` to `0` to reset counter", false);
                    embedBuilder.addField(pre + "backup pause get", "Print the amount of backups to be skipped", false);
                    embedBuilder.addBlankField(false);
                }
                embedBuilder.addField(pre + "help", "Print this message", false);
                returnChannel.sendMessageEmbeds(embedBuilder.build()).queue();

            } else if (msg.getContentRaw().equals(pre + "status") && permissions.getPermissions("status", event)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Minecraft Server status", null);
                if(Main.serverThread.serverStarted) {
                    MineStat ms = new MineStat(realIP, pingPort);
                    if(ms.isServerUp()) {
                        try {
                            QueryResponse fullTest = new MCQuery(realIP, queryPort).fullStat();
                            embedBuilder.setColor(Green);
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

            } else if(msg.getContentRaw().startsWith(pre + "backup") && backupTime != 0) {
                if(msg.getContentRaw().startsWith(pre + "backup restore") && permissions.getPermissions("backup", event)) {
                    String ID = msg.getContentRaw().substring(15 + pre.length());

                    if(!Main.serverThread.serverRunning && git.commitIDRegex.matcher(ID).find()) {
                        String rollbackReturnString = git.rollBack(ID);

                        if(rollbackReturnString.startsWith("HEAD is now at " + ID)) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Green).setTitle("Backup restore").setDescription("Server rolled back to commit " + ID).build()).queue();

                        } else if(rollbackReturnString.contains("fatal: Cannot do hard reset with paths.")) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("Backup restore").setDescription("Invalid Commit ID. It probably has a space in it").build()).queue();
                        } else if(rollbackReturnString.contains("unknown revision or path not in the working tree.")) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("Backup restore").setDescription("The specified commit ID was not found").build()).queue();
                        } else {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("Backup restore").setDescription(rollbackReturnString).build()).queue();
                        }


                    } else if(Main.serverThread.serverRunning) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("Backup restore").setDescription("Please stop the server first").build()).queue();

                    } else {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.red).setTitle("Backup restore").setDescription("Please specify a valid commit id to roll back to. It should be 4-40 digits of base 62").build()).queue();
                    }

                } else if(msg.getContentRaw().startsWith(pre + "backup pause") && permissions.getPermissions("backup", event)) {
                    if(msg.getContentRaw().startsWith(pre + "backup pause get")) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setDescription(git.backupPauseAmount + " Skipped backups left").build()).queue();

                    } else {
                        try {
                            int check = Integer.parseInt(msg.getContentRaw().substring(13 + pre.length()));
                            if (check < 0) {
                                returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup pause").setColor(Color.red).setDescription("Negative Integer. Please provide a non-negative integer").build()).queue();

                            } else {
                                git.backupPauseAmount = check;
                                returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup pause").setColor(Green).setDescription(check + " backup(s) paused").build()).queue();

                            }
                        } catch (NumberFormatException e) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup pause").setColor(Color.red).setDescription("Invalid Integer. Please type a valid integer").build()).queue();

                        } catch (IndexOutOfBoundsException e) {
                            returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup pause").setColor(Color.red).setDescription("No value provided. Please specify a valid integer").build()).queue();
                        }

                    }
                } else if(msg.getContentRaw().startsWith(pre + "backup save") && permissions.getPermissions("backupSave", event)) {

                    if(msg.getContentRaw().length() <= 12 + pre.length()) {
                        returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup save").setColor(Color.red).setDescription("Please specify a commit comment").build()).queue();

                    } else {
                        List<String> retur = git.backup(msg.getContentRaw().substring(12 + pre.length()));
                        StringBuilder ss =  new StringBuilder();
                        for (String s : retur) {
                            ss.append(s);
                            ss.append("\n");
                        }

                        EmbedBuilder returnEmbed = new EmbedBuilder().setTitle("Backup save").setDescription(ss.toString());

                        if(ss.toString().contains("Backup complete on")) {
                            returnEmbed.setColor(Green);
                        } else {
                            returnEmbed.setColor(Color.red);
                        }
                        returnChannel.sendMessageEmbeds(returnEmbed.build()).queue();
                    }

                } else if (permissions.getPermissions("backup", event) || permissions.getPermissions("backupSave", event)) {
                    returnChannel.sendMessageEmbeds(new EmbedBuilder().setTitle("Backup").setColor(Color.red).setDescription("Please specify a subcommand").build()).queue();
                }

            } else {
                //System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                //System.out.println("Content: " + msg.getContentDisplay());

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && permissions.getPermissions("server", event)) {
                    Main.serverThread.commandUse(msg.getContentRaw());
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
                            .put("text", event.getMember().getEffectiveName())
                            .put("color", rgb)
                            .put("hoverEvent", new JSONObject()
                                    .put("action", "show_text")
                                    .put("value", msg.getAuthor().getAsTag())
                            )
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

                        String replyAuthorColor = "";
                        String name = "";
                        if (!replyMessage.isWebhookMessage()) {
                            Member member = event.getGuild().retrieveMember(replyMessage.getAuthor()).complete();
                            name = member.getEffectiveName();

                            try {
                                Color c = member.getColor();
                                int R = c.getRed();
                                int G = c.getGreen();
                                int B = c.getBlue();

                                replyAuthorColor =  "#" + Integer.toHexString(R) + Integer.toHexString(G) + Integer.toHexString(B);
                            } catch (NullPointerException e) {
                                replyAuthorColor = "#FFFFFF";
                            }
                        } else {
                            name = replyMessage.getAuthor().getName();
                            replyAuthorColor = "#FFFFFF";
                        }

                        command.put(new JSONObject()
                                .put("clickEvent", new JSONObject()
                                        .put("action", "open_url")
                                        .put("value", replyMessage.getJumpUrl())
                                )
                                .put("hoverEvent", new JSONObject()
                                        .put("action", "show_text")
                                        .put("value", "Click to open the message\n" + replyMessage.getAuthor().getAsTag())
                                )
                                .put("underlined" , "true")
                                .put("color", replyAuthorColor)
                                .put("text", name)
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
                        if(!msg.getContentRaw().isEmpty()) command.put(new JSONObject()
                                .put("color", "gray")
                                .put("text", " | ")
                        );
                        if(msg.getAttachments().size() > 1) command.put(new JSONObject()
                                .put("color", "gray")
                                .put("text", "Attachments: ")
                        );
                        else command.put(new JSONObject()
                                .put("color", "gray")
                                .put("text", "Attachment: ")
                        );
                        //else if(msg.getContentRaw().length() == 0) System.out.println("length 0");
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

                    Main.serverThread.commandUse("/execute if entity @a run tellraw @a " + command.toString());
                }
            }
        }
    }
}