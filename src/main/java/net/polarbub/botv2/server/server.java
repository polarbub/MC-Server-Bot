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

    public boolean serverRunning = false;
    public boolean serverStarted = false;

    public List<String> players = new ArrayList<>();

    public static Pattern playerJoinPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: ([0-9A-z_]{3,16}) joined the game");
    public static Pattern playerLeavePattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[Server thread\\/INFO\\]: ([0-9A-z_]{3,16}) left the game");

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

        boolean found = false;

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


                found = false;
                //Detect when someone dies
                Matcher matcher3 = playerJoinPattern.matcher(line);
                if(matcher3.find()) {
                    players.add(matcher3.group(1));
                    found = true;
                }

                Matcher matcher4 = playerLeavePattern.matcher(line);
                if(matcher4.find()) {
                    players.remove(matcher4.group(1));
                    found = true;
                }


                if (line.length() >= 33) {
                    String trimmedLine = line.substring(33);
                    for (String player : players) if (!found && trimmedLine.startsWith(player)) {
                        chatBridgeChannel.sendMessageFormat(trimmedLine).queue();
                    }
                }


                if(Main.stopHard) break;
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        serverStarted = false;

        if (config.backupTime != 0)  {
            if(!git.inSleep) {
                git.stopGit = true;
            }

            if(git.inSleep) {
                Main.gitThread.stop();
            }

            Main.gitThread = new git();

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
        }

        serverRunning = false;
    }
}