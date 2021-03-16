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
            Main.commandUse(tosay);
        }

    }

}
