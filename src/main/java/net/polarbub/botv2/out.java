package net.polarbub.botv2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class out extends Thread{
    //Message Cacheing
    public static String Final = "";
    public static String chatBridgeFinal = "";
    public static String temp;
    public static String chatBridgeTemp;
    public static boolean inUse = false;
    public static boolean chatBridgeInUse = false;
    public void run() {
        while(true) {
            if(!inUse && !Final.equals("")) {
                Main.consoleChannel.sendMessage(Final).queue();
                Final = "";
            }
            if(!chatBridgeInUse && !chatBridgeFinal.equals("")) {
                Main.chatBridgeChannel.sendMessage(chatBridgeFinal).queue();
                chatBridgeFinal = "";
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
            Main.consoleChannel.sendMessageFormat("This message is too long to send :(").queue();
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
            Main.consoleChannel.sendMessageFormat(Final).queue();
            Final = message;
        }
        inUse = false;
        addChatBridge(message);
    }

    public static void addChatBridge(String messageRaw) {
        if(Main.serverRunning) {
            for(int i = 0; i < Main.pattern.length; i++) {
                Matcher matcher = Main.pattern[i].matcher(messageRaw);
                if(matcher.matches()) {
                    //System.out.print(i + " ");
                    String message = matcher.group(1);
                    chatBridgeInUse = true;
                    if(message.length() >= 2000) {
                        Main.chatBridgeChannel.sendMessageFormat("This message is too long to send :(").queue();
                    }
                    chatBridgeTemp = String.join("",chatBridgeFinal, "\n", message);
                    if(chatBridgeTemp.length() <= 2002) {
                        chatBridgeFinal = chatBridgeTemp;
                    } else {
                        Main.chatBridgeChannel.sendMessageFormat(chatBridgeFinal).queue();
                        chatBridgeFinal = message;
                    }
                    chatBridgeInUse = false;
                    break;
                }
            }
        }

    }

}