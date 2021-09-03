package net.polarbub.botv2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.*;

public class outChatBridge {
    public static boolean InUse = false;
    public static String Temp;
    public static String Final = "";

    private static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    private static void processPatternSystem(Pattern pattern, String messageRaw) {
        Matcher matcher = pattern.matcher(messageRaw);
        if(matcher.matches()) {
            String message = matcher.group(1);
            if(message.length() >= 2000) {
                chatBridgeChannel.sendMessageFormat("This message is too long to send").queue();
            }
            Temp = String.join("",Final, "\n", message);
            if(Temp.length() <= 2002) {
                Final = Temp;
            } else {
                chatBridgeChannel.sendMessageFormat(Final).queue();
                Final = message;
            }
        }
    }

    private static void sendWebHookMessage(String Username, String Message) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) webHookURL.openConnection();

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

    private static void processPatternNamed(Pattern pattern, String messageRaw) {
        if(!pattern.toString().equals("")) {
            Matcher matcher = pattern.matcher(messageRaw);
            if(matcher.matches()) {
                try {
                    sendWebHookMessage(matcher.group(4), matcher.group(5));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void add(String messageRaw) {
        if(Main.serverRunning) {
            InUse = true;

            processPatternNamed(chatBridgePattern, messageRaw);

            for(Pattern pattern : joinLeavePattern) {
                processPatternSystem(pattern, messageRaw);
            }
            InUse = false;
        }

    }
}

class patternArray {
    public Pattern[] array;
    public int nameGroup;
    public int dataGroup;
}