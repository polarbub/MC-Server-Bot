package net.polarbub.botv2.message;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.server.server;

import java.util.NoSuchElementException;
import java.util.Scanner;

import static net.polarbub.botv2.Main.toBytes;
import static net.polarbub.botv2.config.config.consoleChannel;

public class in extends Thread{
    public static String tosay;
    public void run() {
        while(true) {
            try {
                Scanner myObj = new Scanner(System.in);  // Create a Scanner object
                tosay = myObj.nextLine();
            } catch (NoSuchElementException ignored) {}

            if(!tosay.equals(""))
            if (tosay.length() >= 2000) {
                consoleChannel.sendFile(toBytes(tosay.toCharArray()), "longmessage.txt").queue();
                //consoleChannel.sendMessageFormat("This message is too long to send").queue();
            } else {
                config.consoleChannel.sendMessageFormat(tosay).queue();
            }

            if(tosay.equalsIgnoreCase("start")) {
                if(Main.serverThread.serverRunning) {
                    out.add("Server is Running");
                } else {
                    Main.serverThread = new server();
                    Main.serverThread.start();
                }
            } else if(Main.serverThread.serverRunning) {
                Main.serverThread.commandUse(tosay);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}