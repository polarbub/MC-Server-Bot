package net.polarbub.botv2.message;

import net.polarbub.botv2.Main;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;

import static net.polarbub.botv2.Main.toBytes;
import static net.polarbub.botv2.config.config.consoleChannel;

public class out extends Thread{
    //Message Cacheing
    public static String Final = "";
    public static String temp;
    public static boolean inUse = false;

    private static synchronized boolean getsetInUse(boolean set, boolean val) {
        if(set) {
            inUse = val;
        }
        return inUse;
    }

    public void run() {
        while(true) {
            if(!getsetInUse(false, false) && !Final.equals("")) {
                getsetInUse(true, true);
                consoleChannel.sendMessage(Final).queue();
                Final = "";
                getsetInUse(true, false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void add(String message) {
        while(getsetInUse(false, false)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        getsetInUse(true, true);
        System.out.println(message);

        Matcher matcher = Main.ipPattern.matcher(message);
        while (matcher.find()) {
            message = matcher.replaceAll("||censored IP||");
        }

        if (message.length() >= 2000) {
            consoleChannel.sendFile(toBytes(message.toCharArray()), "longmessage.txt").queue();
            //consoleChannel.sendMessageFormat("This message is too long to send").queue();
            return;
        }

        temp = Final + "\n" + message;
        if (temp.length() < 2000) {
            Final = temp;
        } else {
            try {
                consoleChannel.sendMessage(Final).queue();
                Final = message;
            } catch (UnknownFormatConversionException ignored) {}
        }
        getsetInUse(true, false);
        outChatBridge.add(message);
    }
}