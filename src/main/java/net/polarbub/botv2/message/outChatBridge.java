package net.polarbub.botv2.message;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.namedPattern;
import net.polarbub.botv2.config.normalPattern;
import net.polarbub.botv2.server.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static net.polarbub.botv2.config.config.*;

public class outChatBridge {
    public static boolean InUse = false;
    //public static String Temp;
    //public static String Final = "";

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

        //ADD: Make this do pictures too
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

            //ADD: Make functional death regex.
            for(namedPattern pattern : namedPatterns) {
                processPatternNamed(pattern, messageRaw);
            }

            for(normalPattern pattern : normalPatterns) {
                processPatternNormal(pattern, messageRaw);
            }
            InUse = false;
        }

    }
}

