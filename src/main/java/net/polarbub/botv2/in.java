package net.polarbub.botv2;

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
            if(!tosay.equals("")) Main.consoleChannel.sendMessageFormat(tosay).queue();
            if(tosay.toLowerCase().equals("start")) {
                if(Main.serverRunning) {
                    out.add("Server is Running rn");
                } else {
                    Main.serverThread.start();
                }
            } else if(Main.serverRunning) {
                server.commandUse(tosay);
            } else if(tosay.toLowerCase().equals("stopbot")) {
                System.exit(0);
            }
        }

    }

}
