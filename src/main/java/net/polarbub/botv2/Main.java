package net.polarbub.botv2;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import javax.security.auth.login.LoginException;
import java.util.Scanner;

public class Main extends ListenerAdapter {
    public static String pre = ".";
    public static String tosay = "start";
    public static MessageChannel sendhere;

    public static void main(String[] args) throws LoginException {
        JDABuilder.createLight("Nzk2NDYyNTExMjkzMDcxMzYw.X_YRhA.t-qv7jVrQ2lauFkKok-tMylECJ8", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(true) {
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            tosay = myObj.nextLine();
            sendhere.sendMessageFormat(tosay).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();
        if (msg.getContentRaw().equals(pre + "hi"))
        {
            System.out.println("hi");
            channel.sendMessageFormat("hi").queue();
        } else if (msg.getContentRaw().equals(pre + "stopbot")) {
            System.exit(0);
        } else {
            if(tosay.equals(msg.getContentRaw())){
                int a = 0;
            } else {
                System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                System.out.println("Content: " + msg.getContentRaw());
            }
        }
        sendhere = channel;
    }
}