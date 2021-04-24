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
            if(tosay.toLowerCase().equals("start")) {
                if(Main.serverRunning) {
                    out.add("Server is Running rn");
                } else {
                    Main.serverThread.start();
                    Main.serverRunning = true;
                    Main.gitThread.start();
                }
            } else {
                Main.commandUse(tosay);
            }
        }

    }

}
