package net.polarbub.botv2.message;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.server.server;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class in extends Thread{
    public static String tosay;
    public void run() {
        while(true) {
            try {
                Scanner myObj = new Scanner(System.in);  // Create a Scanner object
                tosay = myObj.nextLine();
            } catch (NoSuchElementException ignored) {
            }
            if(!tosay.equals("")) config.consoleChannel.sendMessageFormat(tosay).queue();
            if(tosay.equalsIgnoreCase("start")) {

                if(server.serverRunning) {
                    out.add("Server is Running");
                } else {
                    Main.serverThread = new server();
                    Main.serverThread.start();
                }
            } else if(server.serverRunning) {
                server.commandUse(tosay);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}