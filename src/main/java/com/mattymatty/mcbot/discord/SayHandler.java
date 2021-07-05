package com.mattymatty.mcbot.discord;

import com.mattymatty.mcbot.Config;
import com.mattymatty.mcbot.Main;
import com.mattymatty.mcbot.minecraft.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.json.*;

import javax.print.attribute.standard.JobStateReasons;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public class SayHandler {
    public static boolean say(Server server, Config config, Message message){
        return say(server,config,message.getMember(),message.getContentStripped(), message.getJumpUrl(),
                message.getAttachments().stream().map(Message.Attachment::getUrl).toArray(String[]::new));
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
