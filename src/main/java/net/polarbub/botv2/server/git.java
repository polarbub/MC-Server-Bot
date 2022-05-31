package net.polarbub.botv2.server;

import net.polarbub.botv2.Main;
import net.polarbub.botv2.message.out;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.polarbub.botv2.config.config.*;
import static net.polarbub.botv2.server.runProg.runProgString;

public class git extends Thread {
    public static boolean gitInUse = false;
    public static boolean stopGit = false;
    public static boolean gitStopped = false;
    public static boolean inSleep = false;
    public static boolean saveReturn = false;
    public static int backupPauseAmount = 0;

    private static final Pattern commitChangeNumberRegex = Pattern.compile("^\\d+ files changed(?:, \\d+ insertions\\(\\+\\))?(?:, \\d+ deletions\\(-\\))?");
    private static final Pattern commitIDRegex = Pattern.compile("^[a-z0-9]{7}");
    private static final Pattern branchRegex = Pattern.compile("\\* ([^\\n]+)");

    //Backup waiting

    public static synchronized boolean getsetInUse(boolean set, boolean val) {
        if(set) {
            gitInUse = val;
        }
        return gitInUse;
    }

    public void run() {
        while(true) {
            inSleep = true;
            try {
                Thread.sleep(backupTime * 1000);
            } catch (InterruptedException ignored) {}

            if(stopGit) break;

            if(backupPauseAmount == 0) {
                Main.serverThread.commandUse("say Backup in " + backupWarn + " seconds.");
                try {
                    Thread.sleep(backupWarn * 1000);
                } catch (InterruptedException ignored) {}
                inSleep = false;
                backup("Timed Backup");
            } else if(backupPauseAmount > 0) {
                backupPauseAmount--;
            } else {
                backupPauseAmount = 0;
            }

            if(stopGit) break;
        }
        gitStopped = true;
    }

    public static boolean rollBack(String ID) {

        git.backup("before rollback");
        while (getsetInUse(false, false)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        getsetInUse(true, true);
        runProg.runProg(new ProcessBuilder("git", "branch", ID + "-rollback" ));
        runProg.runProg(new ProcessBuilder("git", "reset", "--hard", ID));
        getsetInUse(true, false);

        return true;
    }

    public static List<String> gitCommit(String comment) {
        String branch = runProgString(new ProcessBuilder("git", "branch", "--show-current"));
        Pattern commitPattern = Pattern.compile("^\\[" + branch + " ([a-z0-9]{4,40})\\] " + comment);

        runProg.runProg(new ProcessBuilder("git", "add", "-A"));

        Process p = runProg.runProgProcess(new ProcessBuilder("git", "commit", "-a", "-m", "\"" + comment + "\""));
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> retur = new ArrayList<>();
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                Matcher matcher = commitPattern.matcher(line);
                Matcher matcher2 = commitChangeNumberRegex.matcher(line);

                if(matcher.find()) {
                    retur.add("Backup complete on branch " + branch + ", with commit id " + matcher.group(1) + ", and with comment \"" + comment + "\"");

                } else if(matcher2.find()) {
                    retur.add(line);
                    //if(server.serverStarted) server.commandUse("say " + line); else out.add(line);

                } else if(line.equals("nothing to commit, working tree clean")) {
                    retur.add("Nothing to backup");
                }
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        if(retur.isEmpty()) {
            retur.add("Failed. Maybe a there was an invalid escape?");
        }

        return retur;
    }

    public static List<String> backup(String comment) {
        while(getsetInUse(false, false)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        getsetInUse(true, true);

        //Stuff to do if the server process in running
        if(Main.serverThread.serverRunning) {
            //Wait for the server to start
            while(!Main.serverThread.serverStarted) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Main.serverThread.commandUse("say Backup Happening");
            Main.serverThread.commandUse("save-off");
            Main.serverThread.commandUse("save-all flush");

            //Wait for the server to finish saving
            saveReturn = false;
            while(!saveReturn) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<String> retur = gitCommit(comment);
        for (String s : retur) {
            Main.serverThread.commandUse("say " + s);
        }
        Main.serverThread.commandUse("save-on");

        if(!Main.serverThread.serverStarted) {
            for (String s : retur) {
                out.add(s);
            }
        }

        getsetInUse(true, false);

        return retur;

    }
}