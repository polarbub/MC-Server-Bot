package net.polarbub.botv2;

import java.io.*;
import java.util.UnknownFormatConversionException;

public class server extends Thread{
    public void run() {
        Main.serverrunning = true;
        try {
            Main.p = Main.pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Main.br = new BufferedReader(new InputStreamReader(Main.p.getInputStream()));
        Main.bw = new BufferedWriter(new OutputStreamWriter(Main.p.getOutputStream()));
        try {
            for (String line = Main.br.readLine(); line != null; line = Main.br.readLine()) {
                System.out.println(line);
                try {
                    Main.ConsoleChannel.sendMessageFormat("%s", line).queue();
                } catch (UnknownFormatConversionException e) {
                    Main.ConsoleChannel.sendMessageFormat("Discord JDA didn't like a char. F").queue();

                }

            }
            Main.p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
        Main.serverrunning = false;
    }

    public static void main() {
        (new server()).start();
    }
}
