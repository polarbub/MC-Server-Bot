package net.polarbub.botv2;

import com.vdurmont.emoji.EmojiParser;
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
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.config.*;

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
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
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
                StringBuilder sb = new StringBuilder("```\n" +  pre + "status                     | get the status of the server\n");
                if(backupTime != 0) sb.append(
                        pre + "backup <backup message>    | backup the server\n" +
                        pre + "backup restore <commit id> | restore backup```"
                ); else sb.append("```");
                sb.append("\nTo send a command to the server send a message in <#" + consoleChannel.getId() + ">. " +
                        "To start the server send `start` in <#" + consoleChannel.getId() + ">. Both these things can also be done in the terminal.\n" +
                        "To send a message through the chat bridge send a message in <#" + chatBridgeChannel.getId() + ">.\n");
                returnChannel.sendMessageFormat(sb.toString()).queue();

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

            } else if(msg.getContentRaw().startsWith(pre + "backup") && backupTime != 0 && permissions.getPermissions("backup", event)) {

                if(msg.getContentRaw().startsWith(pre + "backup restore")) {
                    if(!server.serverRunning && msg.getContentRaw().length() >= 15 + pre.length() && msg.getContentRaw().length() <= 22) {
                        git.rollBack(msg.getContentRaw().substring(15 + pre.length()));
                    } else if(server.serverRunning) {
                        returnChannel.sendMessageFormat("Please stop the server first").queue();
                    } else {
                        returnChannel.sendMessageFormat("Please specify a commit id to roll back to. It should be 7 digits of base 62").queue();
                    }
                } else if(msg.getContentRaw().startsWith(pre + "backup")) {
                    if(msg.getContentRaw().length() <= 7 + pre.length()) {
                        returnChannel.sendMessageFormat("Please specify a commit comment").queue();
                    } else {
                        returnChannel.sendMessageFormat(git.backup(msg.getContentRaw().substring(7 + pre.length()))).queue();
                    }
                }

            } else {
                System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                System.out.println("Content: " + msg.getContentDisplay());

                if (String.valueOf(returnChannel).equals(String.valueOf(consoleChannel)) && server.serverRunning && permissions.getPermissions("server", event)) {
                    server.commandUse(msg.getContentRaw());
                //} else if (String.valueOf(returnChannel).equals(String.valueOf(chatBridgeChannel)) && server.serverRunning && permissions.getPermissions("chatbridge", event)) {
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
                    //ADD: Change flag emoji to use tags
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