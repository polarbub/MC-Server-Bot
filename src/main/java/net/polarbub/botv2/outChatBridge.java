package net.polarbub.botv2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.json.*;

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

    private static void processPatternNormal(normalPattern pattern, String messageRaw) {
        Matcher matcher = pattern.pattern.matcher(messageRaw);
        if(matcher.matches()) {
            String message = matcher.group(pattern.dataGroup);
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

    private static void processPatternNamed(namedPattern pattern, String messageRaw) {
        if(!pattern.toString().equals("")) {
            Matcher matcher = pattern.pattern.matcher(messageRaw);
            if(matcher.matches()) {
                try {
                    sendWebHookMessage(matcher.group(pattern.nameGroup), matcher.group(pattern.dataGroup));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void add(String messageRaw) {
        if(Main.serverRunning) {
            InUse = true;

            for(namedPattern pattern : namedPatterns) {
                processPatternNamed(pattern, messageRaw);
            }

            for(normalPattern pattern : normalPatterns) {
                processPatternNormal(pattern, messageRaw);
            }
            InUse = false;
        }

    }

    public static boolean say(Server server, Config config, Member member, String message, String link, String[] file_url){
        JSONArray msgjson = new JSONArray();
        msgjson.put("");
        JSONObject jobj = new JSONObject()
                .put("text","[DISCORD]")
                .put("color","#7289DA");
        if(Objects.nonNull(link)){
            jobj.put("hoverEvent",new JSONObject()
                    .put("action","show_text")
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("text","Open on Discord")
                                    .put("italic",true)
                                    .put("undelined",true)
                                    .put("color","#7289DA")
                            )
                    )
            );
            jobj.put("clickEvent",new JSONObject()
                    .put("action","open_url")
                    .put("value", link)
            );
        }
        msgjson.put(jobj);
        msgjson.put(new JSONObject()
                .put("text"," <")
        );

        Color color = Optional.ofNullable(member.getColor()).orElse(Color.WHITE);

        String red = Integer.toHexString(color.getRed());
        String green = Integer.toHexString(color.getGreen());
        String blue = Integer.toHexString(color.getBlue());

        if (red.length() == 1) red = "0" + red;
        if (green.length() == 1) green = "0" + green;
        if (blue.length() == 1) blue = "0" + blue;

        String hexColor = "#" + red + green + blue;
        msgjson.put(new JSONObject()
                .put("text",member.getEffectiveName())
                .put("color",hexColor)
                .put("hoverEvent",new JSONObject()
                        .put("action","show_text")
                        .put("contents", new JSONArray()
                                .put(new JSONObject()
                                        .put("text",member.getUser().getAsTag())
                                        .put("undelined",true)
                                        .put("color","#7289DA")
                                )
                        )
                )

        );

        msgjson.put(new JSONObject()
                .put("text","> ")
        );

        if (Objects.nonNull(file_url) && file_url.length>0){
            for (String url: file_url) {
                jobj = new JSONObject();
                jobj.put("text","[");
                jobj.put("extra",new JSONArray()
                        .put(new JSONObject()
                                .put("text","File")
                                .put("underlined",true)
                        ).put(new JSONObject()
                                .put("text","] ")
                        )
                );
                jobj.put("color","#7289DA");
                jobj.put("hoverEvent",new JSONObject()
                        .put("action","show_text")
                        .put("contents", new JSONArray()
                                .put(new JSONObject()
                                        .put("text","Open File in Browser")
                                        .put("undelined",true)
                                        .put("color","#7289DA")
                                )
                        )
                );
                jobj.put("clickEvent",new JSONObject()
                        .put("action","open_url")
                        .put("value", url)
                );
                msgjson.put(jobj);
            }
        }

        msgjson.put(new JSONObject()
                .put("text",message));

        String command = config.MC_SERVER.say_format;
        command = command.replace("%username%",member.getEffectiveName());
        command = command.replace("%color%",hexColor);
        command = command.replace("%message%",JSONWriter.valueToString(message));
        command = command.replace("%messageJSON%",msgjson.toString());

        return server.command(command);
    }
}

