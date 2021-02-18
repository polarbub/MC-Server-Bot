package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class in extends Thread{
    public static String tosay = "start";
    public void run() {
        while(true) {
            try {
                Scanner myObj = new Scanner(System.in);  // Create a Scanner object
                tosay = myObj.nextLine();
                //essageChannel senderChannel = Main.ReturnChannel;
                //senderChannel.sendMessageFormat(tosay).queue();
            } catch (NoSuchElementException e) {
            }
            OutputStream os = Main.p.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            try {
                bw.write(tosay);
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main() {
        (new in()).start();
    }

}
