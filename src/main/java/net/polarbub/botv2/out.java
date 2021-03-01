package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

public class out {
    public static void output(Message msg, MessageReceivedEvent event) {
        if(in.tosay.equals(msg.getContentRaw())){
            int a = 0;
        } else {
            System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
            System.out.println("Content: " + msg.getContentRaw());
        }
    }

}
