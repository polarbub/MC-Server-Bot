package net.polarbub.botv2.server;

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
    public void run() {
        while(true) {
            inSleep = true;
            try {
                Thread.sleep(backupTime * 1000);
            } catch (InterruptedException ignored) {}
            inSleep = false;

            if(stopGit) break;

            if(backupPauseAmount == 0) {
                server.commandUse("say Backup in " + backupWarn + " seconds.");
                try {
                    Thread.sleep(backupWarn * 1000);
                } catch (InterruptedException ignored) {}
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
        Matcher matcher = commitIDRegex.matcher(ID);
        if(!matcher.matches()) {
            return false;
        }

        git.backup("before rollback");
        while (git.gitInUse) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        git.gitInUse = true;
        runProg.runProg(new ProcessBuilder("git", "branch", ID + "-rollback" ));
        runProg.runProg(new ProcessBuilder("git", "reset", "--hard", ID));
        git.gitInUse = false;

        return true;
    }

    public static List<String> gitCommit(String comment) {
        Matcher branchMatcher = branchRegex.matcher(runProgString(new ProcessBuilder("git", "branch")));
        branchMatcher.find();
        String branch = branchMatcher.group(1);

        Pattern commitPattern = Pattern.compile("^\\[" + branch + " ([a-z0-9]{7})\\] " + comment);

        List<String> options = new ArrayList<>();
        options.add("git");
        options.add("commit");
        options.add("-a");
        options.add("-m");
        options.add("\"" + comment + "\"");

        runProg.runProg(new ProcessBuilder("git", "add", "-A"));

        ProcessBuilder pb = new ProcessBuilder(options);
        pb.redirectErrorStream(true);
        Process p = runProg.runProgProcess(pb);

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> retur = new ArrayList<>();
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                Matcher matcher = commitPattern.matcher(line);
                Matcher matcher2 = commitChangeNumberRegex.matcher(line);

                if(matcher.find()) {
                    retur.add("Backup compete on branch " + branch + ", with commit id " + matcher.group(1));

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
        return retur;
    }

    public static List<String> backup(String comment) {
        while(gitInUse) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        gitInUse = true;

        //Stuff to do if the server process in running
        if(server.serverRunning) {
            //Wait for the server to start
            while(!server.serverStarted) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            server.commandUse("say Backup Happening!");
            server.commandUse("save-off");
            server.commandUse("save-all flush");

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

        server.commandUse("say Backup started");

        List<String> retur = gitCommit(comment);
        for (String s : retur) {
            server.commandUse("say " + s);
        }
        server.commandUse("save-on");

        gitInUse = false;
        return retur;
    }
}