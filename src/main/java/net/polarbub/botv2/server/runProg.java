package net.polarbub.botv2.server;

import net.polarbub.botv2.config.config;
import net.polarbub.botv2.message.out;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class runProg {
    //sends lines to out
    public static void runProg(ProcessBuilder pb, String dir) {
        Process p = runProgProcess(pb, dir);
        readOut(p);
    }

    public static void runProg(ProcessBuilder pb) {
        Process p = runProgProcess(pb);
        readOut(p);
    }

    private static void readOut(Process p) {
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

    public static String runProgString(ProcessBuilder pb, String dir) {
        Process p = runProgProcess(pb, dir);
        return readOutString(p);
    }

    public static String runProgString(ProcessBuilder pb) {
        Process p = runProgProcess(pb);
        return readOutString(p);
    }

    private static String readOutString(Process p) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                stringBuilder.append(line);
            }
            p.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

        }
        //stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    //returns the process and has a dir set
    public static Process runProgProcess(ProcessBuilder pb, String dir) {
        pb.directory(new File(dir));
        Process p = runProcessBuilder(pb);
        return p;

    }

    //returns the process and has no dir set
    public static Process runProgProcess(ProcessBuilder pb) {
        pb.directory(new File(config.serverDir));
        Process p = runProcessBuilder(pb);
        return p;
    }


    public static Process runProcessBuilder(ProcessBuilder pb) {
        pb.redirectErrorStream(true);

        if(System.getProperty("os.name").equals("Linux")) {
            List<String> cache = new ArrayList<>();

            cache.add("/usr/bin/bash");
            cache.add("-c");

            StringBuilder sb = new StringBuilder();

            for(String s : pb.command()) {
                sb.append(s);
                sb.append(" ");
            }

            sb.delete(sb.length() - 1,sb.length());

            cache.add(sb.toString());
            pb.command(cache);
        }

        Process p = null;

        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return p;
    }
}