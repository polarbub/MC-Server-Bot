package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.Message;

public class out extends Thread{
    public static String Final = "";
    public static String temp;
    public static boolean inUse = false;
    public void run() {
        while(true) {
            if(!inUse && !Final.equals("")) {
                Main.consoleChannel.sendMessage(Final).queue();
                Final = "";
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main() {
        (new out()).start();
    }

    public static void add(String message) {
        inUse = true;
        if(message.length() >= 2000) {
            Main.consoleChannel.sendMessageFormat("This message is too long to send :(").queue();
        }
        temp = String.join("",Final, "\n", message);
        if(temp.length() <= 2002) {
            Final = temp;
        } else {
            Main.consoleChannel.sendMessageFormat(Final).queue();
            Final = message;
        }
        inUse = false;
    }
}