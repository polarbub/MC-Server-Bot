package net.polarbub.botv2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class git extends Thread{
    public static boolean gitInUse = false;

    public void run() {

    }

    public static void backup(String comment) {
        gitInUse = true;
        System.out.println("Backup started");
        out.add("Backup started");
        runProg("git add *.*");
        if(comment == null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            runProg("git commit -a -c " + dtf.format(now));
        } else {
            runProg("git commit " + comment);
        }
        gitInUse = false;
        System.out.println("Backup complete");
        out.add("Backup complete");
    }

    public static void runProg(String prog) {
        ProcessBuilder pb = new ProcessBuilder(prog);
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                System.out.println(line);
                out.add(line);
            }
            Main.p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
    }

    public static void main() {
        (new git()).start();
    }
}
