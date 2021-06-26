package net.polarbub.botv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class git extends Thread {
    public static boolean gitInUse = false;
    public static boolean stopGit = false;
    public static boolean gitStopped = false;
    public static boolean inSleep = false;
    public static boolean autoSaveReturn = false;

    //Backup waiting
    public void run() {
        while(true) {
            inSleep = true;
            try {
                Thread.sleep(Main.backupTime * 1000);
            } catch (InterruptedException ignored) {}
            if(Main.serverRunning) server.commandUse("say Backup in " + Main.backupWarn + "seconds."); {
                try {
                    Thread.sleep(Main.backupWarn * 1000);
                } catch (InterruptedException ignored) {}
                inSleep = false;
            }
            while(gitInUse) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            if(Main.serverRunning) server.commandUse("say Backup Happening!");
            if(Main.serverRunning) server.commandUse("save-off");
            if(Main.serverRunning) server.commandUse("save-all flush");
            autoSaveReturn = false;
            while(!autoSaveReturn) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}

            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            backup("Timed Backup");
            if(Main.serverRunning) server.commandUse("save-on");
            if(stopGit) break;
        }
        gitStopped = true;
    }

    //Backup thingy
    public static void backup(String comment) {
        while(gitInUse) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        gitInUse = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runProg(new ProcessBuilder("git", "add", "-A"), Main.serverDir);
        out.add("\nBackup started");
        if(comment == null) {
            runProg(new ProcessBuilder("git", "commit", "-a", "-m", "\"No Comment\""), Main.serverDir);
        } else {
            runProg(new ProcessBuilder("git", "commit", "-a", "-m", "\"" + comment + "\""), Main.serverDir);
        }
        gitInUse = false;
        out.add("Backup complete\n");
    }

    //Run a program using process builder and print its output
    public static void runProg(ProcessBuilder pb, String dir) {
        pb.directory(new File("server\\"));
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert p != null;
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                out.add(line);
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
    }
}