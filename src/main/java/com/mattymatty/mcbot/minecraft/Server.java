package com.mattymatty.mcbot.minecraft;

import com.mattymatty.mcbot.Config;
import com.mattymatty.mcbot.DataListener;
import com.mattymatty.mcbot.Main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {
    private static final boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");
    public static final int MAX_QUEUE_SIZE = 1000;
    private Process server;
    private final Config config;
    private OutputStreamWriter os;
    private Set<DataListener> chatListeners = new LinkedHashSet<>();
    private Set<DataListener> consoleListeners = new LinkedHashSet<>();
    private Set<DataListener> errorListeners = new LinkedHashSet<>();
    private Set<DataListener> startListeners = new LinkedHashSet<>();
    private Set<DataListener> stopListeners = new LinkedHashSet<>();
    private Set<DataListener> saveListeners = new LinkedHashSet<>();

    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Semaphore messageSem = new Semaphore(0);

    public Server(Config config) {
        this.config = config;
        Thread shutdown = new Thread( () -> {
            if(this.isRunning()){
                if(!this.stop()) {
                    server.destroy();
                    try {
                        if(!server.waitFor(30, TimeUnit.SECONDS))
                            server.destroyForcibly();
                    } catch (InterruptedException ignored) {}

                }
            }
        },"Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdown);
        new Thread(this::parseMessages,"Message Consumer").start();
    }

    public boolean start(){
        try {
            if (server == null || !server.isAlive()) {
                ProcessBuilder pb = new ProcessBuilder();
                if (isWindows) {
                    pb.command("cmd.exe", "/c", config.MC_SERVER.startCMD);
                } else {
                    pb.command("sh", "-c", config.MC_SERVER.startCMD);
                }
                pb.redirectErrorStream(false);
                server = pb.start();
                os = new OutputStreamWriter(server.getOutputStream());
                new Thread(this::readOut,"Output Listener").start();
                new Thread(this::readErr,"Error Listener").start();
                new Thread(()->{
                    try {
                        server.waitFor();
                        stopListeners.forEach(l->l.listen("Stopped"));
                        Main.LOG.print("[Server] server stopped");
                        if(error.length() > old_length)
                            errorListeners.forEach(l -> {
                                try {
                                    l.listen(error.toString());
                                } catch (Exception ignored) {
                                }
                            });
                        error.setLength(0);
                    } catch (InterruptedException ignored) {}
                },"Stop Listener").start();
                return true;
            }
        }catch (Exception ex){
            return false;
        }
        return false;
    }

    public boolean stop(){
        try {
            if (this.command("stop")) {
                server.waitFor(30,TimeUnit.SECONDS);
                return true;
            }
            return false;
        }catch (InterruptedException ignored){
            return false;
        }
    }

    public boolean command(String cmd){
        try {
            if (server != null && server.isAlive()) {
                os.write(cmd + "\r\n");
                os.flush();
                return true;
            }
            return false;
        }catch (Exception ex){
            return false;
        }
    }

    public boolean isRunning(){
        return (server!=null && server.isAlive());
    }

    public Server addChatListener(DataListener listener){
        chatListeners.add(listener);
        return this;
    }

    public Server addConsoleListener(DataListener listener){
        consoleListeners.add(listener);
        return this;
    }

    public Server addErrorListener(DataListener listener){
        errorListeners.add(listener);
        return this;
    }

    public Server addStartListener(DataListener listener){
        startListeners.add(listener);
        return this;
    }

    public Server addStopListener(DataListener listener){
        stopListeners.add(listener);
        return this;
    }

    public Server addSaveListener(DataListener listener){
        saveListeners.add(listener);
        return this;
    }

    public Server removeChatListener(DataListener listener){
        chatListeners.remove(listener);
        return this;
    }

    public Server removeConsoleListener(DataListener listener){
        consoleListeners.remove(listener);
        return this;
    }

    public Server removeErrorListener(DataListener listener){
        errorListeners.remove(listener);
        return this;
    }

    public Server removeStartListener(DataListener listener){
        startListeners.remove(listener);
        return this;
    }

    public Server removeStopListener(DataListener listener){
        stopListeners.remove(listener);
        return this;
    }

    public Server removeSaveListener(DataListener listener){
        saveListeners.remove(listener);
        return this;
    }

    private StringBuffer error = new StringBuffer();
    private int old_length = 0;
    private void parseMessages(){
        try{
            List<Pattern> chat_patterns = Arrays.stream(config.MC_SERVER.chat_regex).map(Pattern::compile).collect(Collectors.toList());
            Pattern save_pattern = Pattern.compile(config.MC_SERVER.save_regex);
            Pattern start_pattern = Pattern.compile(config.MC_SERVER.done_regex);
            Pattern error_pattern = Pattern.compile(config.MC_SERVER.error_regex);

            while(!Thread.interrupted()){
                messageSem.acquire();
                String line = messageQueue.poll();
                if (line == null) continue;
                if(error_pattern.matcher(line).matches()) {

                    if(error.length() > old_length)
                        errorListeners.forEach(l -> {
                            try {
                                l.listen(error.toString());
                            } catch (Exception ignored) {
                            }
                        });
                    error.setLength(0);
                    error.append(line).append("\n");
                    old_length = error.length();

                    consoleListeners.forEach(l -> {
                        try {
                            l.listen(line);
                        } catch (Exception ignored) {
                        }
                    });

                    Optional<Matcher> match = chat_patterns.stream().map(p->p.matcher(line)).filter(Matcher::matches).findAny();
                    match.ifPresent(matcher -> chatListeners.forEach(l->{
                        try {
                            l.listen(match.get().group(1));
                        }catch (Exception ignored){}
                    }));

                    Matcher matcher = save_pattern.matcher(line);
                    if(matcher.matches()){
                        saveListeners.forEach(l->{
                            try {
                                l.listen(line);
                            }catch (Exception ignored){}
                        });
                    }

                    matcher = start_pattern.matcher(line);
                    if(matcher.matches()){
                        Main.LOG.print("[Server] server started");
                        startListeners.forEach(l->{
                            try {
                                l.listen(line);
                            }catch (Exception ignored){}
                        });
                    }

                } else {
                    error.append(line).append("\r\n");
                }

            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private void readOut(){
        try{
            BufferedReader stdout = new BufferedReader(new InputStreamReader(server.getInputStream()));

            while(!Thread.interrupted() && server.isAlive()){
                String l = stdout.readLine();
                if(messageQueue.size()< MAX_QUEUE_SIZE && l != null) {
                    messageQueue.add(l);
                    messageSem.release();
                }
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private void readErr(){
        try{
            BufferedReader stderr = new BufferedReader(new InputStreamReader(server.getErrorStream()));

            while(!Thread.interrupted() && server.isAlive()){
                String l = stderr.readLine();
                if( l != null) {
                    error.append(l).append('\n');
                }
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
