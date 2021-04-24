package net.polarbub.botv2;

import java.io.*;

public class server extends Thread{
    //like git.runprog just to discord with cacheing
    public void run() {
        //start server
        Main.serverRunning = true;
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
            }
            Main.p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }

        //Stop git
        if(git.inSleep) Main.gitThread.stop(); else git.stopGit = true;
        while(!git.gitStopped) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        git.backup("Server Shutdown");
        while(git.autoBackup) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //Stop server
        Main.serverRunning = false;
        //git.runProg("git");
    }
}