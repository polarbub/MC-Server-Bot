package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class in extends Thread{
    public static String tosay = "start";
    public void run() {
        while(true) {
            try {
                Scanner myObj = new Scanner(System.in);  // Create a Scanner object
                tosay = myObj.nextLine();
            } catch (NoSuchElementException ignored) {
            }
            if (Main.serverRunning) {
                Main.bw = new BufferedWriter(new OutputStreamWriter(Main.p.getOutputStream()));
                try {
                    Main.bw.write(tosay);
                    Main.bw.newLine();
                    Main.bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main() {
        (new in()).start();
    }

}
