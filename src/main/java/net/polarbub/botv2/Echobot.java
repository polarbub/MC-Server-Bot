package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Scanner;

public class Echobot extends Thread{
    public static String tosay = "start";
    public static MessageChannel sendhere;
    public void run() {
        while(true) {
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            tosay = myObj.nextLine();
            sendhere.sendMessageFormat(tosay).queue();
        }

    }
    public static void out(Message msg, MessageReceivedEvent event) {
        if(tosay.equals(msg.getContentRaw())){
            int a = 0;
        } else {
            System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
            System.out.println("Content: " + msg.getContentRaw());
        }
    }

    public static void main() {
        (new Echobot()).start();
    }

}

/*public class MainThreadClass {
    public static void main(String[] args) {
        Echobot input = new input();
        Thread t = new Thread(input);
        t.start();
    }
}*/
