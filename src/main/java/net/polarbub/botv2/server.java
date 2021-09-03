package net.polarbub.botv2;

import java.io.*;
import static net.polarbub.botv2.config.*;

public class server extends Thread{
    //Send a command to the server
    public static void commandUse(String command) {
        if (Main.serverRunning) {
            System.out.println(command);
            try {
                Main.bw.write(command);
                Main.bw.newLine();
                Main.bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //like git.runprog just to discord with cacheing
    public void run() {
        //start server
        try {
            Main.p = Main.pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //init console reader
        Main.br = new BufferedReader(new InputStreamReader(Main.p.getInputStream()));
        Main.bw = new BufferedWriter(new OutputStreamWriter(Main.p.getOutputStream()));

        //Listen for messages
        try {
            for (String line = Main.br.readLine(); line != null; line = Main.br.readLine()) {
                out.add(line);
                if(line.contains("] [Server thread/INFO]: Saved the game") && !line.contains("<") && !line.contains(">")) {
                    git.autoSaveReturn = true;
                }
                if(line.contains("] [Server thread/INFO]: Done (") && line.contains(")! For help, type ") && !Main.serverRunning && backupTime > 0) {
                    Main.serverRunning = true;
                    Main.gitThread.start();
                }
            }
            Main.p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }

        //Stop git
        if(git.inSleep) {Main.gitThread.stop(); } else {
            git.stopGit = true;
            while (!git.gitStopped) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        git.backup("Server Shutdown");
        while(git.gitInUse) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        out.add("Server stopped\n");

        //Stop server
        Main.serverRunning = false;
    }
}