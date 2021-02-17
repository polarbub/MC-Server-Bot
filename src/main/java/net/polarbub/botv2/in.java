package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Scanner;

public class in extends Thread{
    public static String tosay = "start";
    public void run() {
        while(true) {
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            tosay = myObj.nextLine();
            MessageChannel senderChannel = Main.ReturnChannel;
            senderChannel.sendMessageFormat(tosay).queue();
        }

    }

    public static void main() {
        (new in()).start();
    }

}
