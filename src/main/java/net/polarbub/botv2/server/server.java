package net.polarbub.botv2.server;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.message.out;
import net.polarbub.botv2.message.outChatBridge;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.config.chatBridgeChannel;

public class server extends Thread {
    public BufferedWriter bw;
    public BufferedReader br;
    public Process p;

    //FIX: Replace these with an enum
    public boolean serverRunning = false;
    public boolean serverStarted = false;

    //Send a command to the server
    public void commandUse(String command) {
        if (this.serverRunning) {
            try {
                bw.write(command);
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        serverRunning = true;

        while(git.getsetInUse(false, false)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ProcessBuilder pb = new ProcessBuilder(config.serverArgs);
        Process p = runProg.runProgProcess(pb);

        bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                out.add(line);

                Matcher matcher = config.gitsavereturnPattern.matcher(line);
                if(matcher.find()) {
                    git.saveReturn = true;
                }

                if(!serverStarted) {
                    Matcher matcher2 = config.startPattern.matcher(line);
                    if(matcher2.find()) {
                        if (config.backupTime != 0) Main.gitThread.start();
                        serverStarted = true;
                    }
                }
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        serverStarted = false;

        if (config.backupTime != 0)  {
            git.stopGit = true;
            Main.gitThread.interrupt();

            while(!git.gitStopped) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //We don't use git.backup() here because if serverRunning is true and serverStarted is false it will wait for
            // the server to start and serverStarted to be set to true before running the backup. The server will never
            // start though because this is after the server has been stopped.
            while(git.getsetInUse(false, false)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            git.getsetInUse(true, true);
            List<String> retur = git.gitCommit("Server Stopped");
            for (String s : retur) {
                out.add(s);
            }

            git.getsetInUse(true, false);

            Main.gitThread = new git();
        }

        serverRunning = false;
    }
}