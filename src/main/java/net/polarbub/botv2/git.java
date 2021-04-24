package net.polarbub.botv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class git extends Thread {
    public static boolean gitInUse = false;
    public static boolean autoBackup = false;
    public static boolean stopGit = false;
    public static boolean gitStopped = false;
    public static boolean inSleep = false;

    //Backup waiting
    public void run() {
        while(true) {
            inSleep = true;
            try {
                Thread.sleep(Main.backupTime * 1000);
            } catch (InterruptedException ignored) {}
            inSleep = false;
            Main.commandUse("say Backup in " + Main.backupWarn);
            Main.commandUse("save-off");
            Main.commandUse("save-all flush");
            autoBackup = true;
            while(gitInUse) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            backup("autosave");
            Main.commandUse("save-on");
            autoBackup = false;
            if(stopGit) break;
        }
        gitStopped = true;
    }

    //Backup thingy
    public static void backup(String comment) {
        gitInUse = true;
        out.add("Backup started");
        runProg("\"git\" \"add\" \"-A\"");
        if(comment == null) {
            runProg("\"git\" \"commit\" \"-m\" \"No Comment\"");
        } else {
            runProg("\"git\" \"commit\" \"-m\" \"" + comment + "\"");
        }
        gitInUse = false;
        out.add("Backup complete");
    }

    //Run a program using process builder and print its output
    public static void runProg(String prog) {
        ProcessBuilder pb = new ProcessBuilder(prog);
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
            Main.p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
    }
}