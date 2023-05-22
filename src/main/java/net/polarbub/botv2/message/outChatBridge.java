package net.polarbub.botv2.message;

import net.dv8tion.jda.api.entities.TextChannel;
import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.namedPattern;
import net.polarbub.botv2.config.normalPattern;
import net.polarbub.botv2.server.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.config.*;
import static net.polarbub.botv2.config.config.chatBridgeChannel;

public class outChatBridge {
    public static boolean InUse = false;

    //public static String Temp;
    //public static String Final = "";

    public static List<String> players = new ArrayList<>();
    public static Pattern playerJoinPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: ([0-9A-z_]{3,16}) joined the game");
    public static Pattern playerLeavePattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: ([0-9A-z_]{3,16}) left the game");
    public static Pattern lostConnectionPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: [0-9A-z_]{3,16} lost connection: ");
    public static Pattern isDeathPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: ([0-9A-z_]{3,16}.+)");

    private static String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    private static void sendWebHookMessage(String Username, String Message) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) webHookURL.openConnection();

        //ADD: Make this do pictures too.
        // The way Discord does per message webhook avatars is by taking url to the photo. I can't really host photos for Discord to grab in this bot
        Map<String, String> parameters = new HashMap<>();
        parameters.put("content", Message);
        parameters.put("username", Username);

        connection.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(getParamsString(parameters));
        out.flush();
        out.close();
        connection.getResponseCode();
    }

    private static void processPatternNormal(normalPattern pattern, String messageRaw) {
        Matcher matcher = pattern.pattern.matcher(messageRaw);
        if(matcher.matches()) {

            String message = matcher.group(pattern.dataGroup);

            if(message.length() >= 2000) {
                chatBridgeChannel.sendMessageFormat("This message is too long to send").queue();
            } else {
                chatBridgeChannel.sendMessageFormat(message).queue();
            }

        }
    }

    private static void processPatternNamed(namedPattern pattern, String messageRaw) {
        Matcher matcher = pattern.pattern.matcher(messageRaw);
        if(matcher.matches()) {
            System.out.println(pattern + " " + messageRaw);
            try {
                sendWebHookMessage(matcher.group(pattern.nameGroup), pattern.prefix + matcher.group(pattern.dataGroup));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void add(String messageRaw) {
        if(Main.serverThread.serverStarted) {
            InUse = true;

            //Detect deaths. This works by keeping a list of the players on the server and when a message starts with one of their names it will print it out.
            boolean found = false;
            Matcher matcher3 = playerJoinPattern.matcher(messageRaw);
            if(matcher3.find()) {
                players.add(matcher3.group(1));
                found = true;
            }

            Matcher matcher4 = playerLeavePattern.matcher(messageRaw);
            if(matcher4.find()) {
                players.remove(matcher4.group(1));
                found = true;
            }

            Matcher matcher5 = lostConnectionPattern.matcher(messageRaw);
            if(matcher5.find()) {
                found = true;
            }

            Matcher matcher6 = isDeathPattern.matcher(messageRaw);
            if (matcher6.find()) {
                String trimmedLine = matcher6.group(1);
                for (String player : players) if (!found && trimmedLine.startsWith(player)) {
                    chatBridgeChannel.sendMessage(trimmedLine).queue();
                }
            }

            //Check against regexes from the config file
            //These go to a webhook, so they can have a name
            for(namedPattern pattern : namedPatterns) {
                processPatternNamed(pattern, messageRaw);
            }

            //These are sent normally
            for(normalPattern pattern : normalPatterns) {
                processPatternNormal(pattern, messageRaw);
            }
            InUse = false;
        }

    }
}

