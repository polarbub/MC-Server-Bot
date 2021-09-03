package net.polarbub.botv2;

import java.util.regex.Matcher;
import static net.polarbub.botv2.config.*;

public class out extends Thread{
    //Message Cacheing
    public static String Final = "";
    public static String temp;
    public static boolean inUse = false;

    public void run() {
        while(true) {
            if(!inUse && !Final.equals("")) {
                consoleChannel.sendMessage(Final).queue();
                Final = "";
            }
            if(!outChatBridge.InUse && !outChatBridge.Final.equals("")) {
                chatBridgeChannel.sendMessage(outChatBridge.Final).queue();
                outChatBridge.Final = "";
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void add(String message) {
        inUse = true;
        System.out.println(message);
        if(message.length() >= 2000) {
            consoleChannel.sendMessageFormat("This message is too long to send :(").queue();
            return;
        }

        Matcher matcher = Main.ipPattern.matcher(message);
        if(matcher.matches()) {
            message = matcher.replaceAll("||ip.no.look.ing||");
        }

        temp = String.join("",Final, "\n", message);
        if(temp.length() <= 2002) {
            Final = temp;
        } else {
            consoleChannel.sendMessageFormat(Final).queue();
            Final = message;
        }
        inUse = false;
        outChatBridge.add(message);
    }
}