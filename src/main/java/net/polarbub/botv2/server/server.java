package net.polarbub.botv2.server;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.message.out;

import java.io.*;
import java.util.regex.Matcher;

public class server extends Thread {
    //public static
    public static BufferedWriter bw;
    public static BufferedReader br;
    public static Process p;
    public static boolean serverRunning = false;
    public static boolean serverStarted = false;

    //Send a command to the server
    public static void commandUse(String command) {
        if (serverRunning) {
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

        while(git.gitInUse) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx1G", "-Xms1G", "fabric-server-launch.jar", "-nogui");
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

                Matcher matcher2 = config.startPattern.matcher(line);
                if(matcher2.find()) {
                    if (config.backupTime != 0) Main.gitThread.start();
                    serverStarted = true;
                }

                if(Main.stopHard) break;
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        serverStarted = false;

        if (config.backupTime != 0)  {
            git.gitInUse = true;
            git.gitCommit("Server Stopped");
            git.gitInUse = false;
        }

        serverRunning = false;

    }
}