package com.mattymatty.mcbot.minecraft;

import com.mattymatty.mcbot.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {
    private Process server;
    private final Config config;
    private OutputStreamWriter os;
    private List<DataListener> chatListeners = new LinkedList<>();
    private List<DataListener> consoleListeners = new LinkedList<>();
    private List<DataListener> errorListeners = new LinkedList<>();
    private List<DataListener> startListeners = new LinkedList<>();
    private List<DataListener> stopListeners = new LinkedList<>();
    private List<DataListener> saveListeners = new LinkedList<>();
    private static final boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");

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
                new Thread(this::parseOut,"Output Listener").start();
                new Thread(this::parseErr,"Error Listener").start();
                new Thread(()->{
                    try {
                        server.waitFor();
                        stopListeners.forEach(l->l.listen("Stopped"));
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

    private void parseOut(){
        try{
            BufferedReader stdout = new BufferedReader(new InputStreamReader(server.getInputStream()));

            List<Pattern> chat_patterns = Arrays.stream(config.MC_SERVER.chat_regex).map(Pattern::compile).collect(Collectors.toList());
            Pattern save_pattern = Pattern.compile(config.MC_SERVER.save_regex);
            Pattern start_pattern = Pattern.compile(config.MC_SERVER.done_regex);

            while(!Thread.interrupted() && server.isAlive()){
                String line = stdout.readLine();
                if (line == null) continue;
                consoleListeners.forEach(l->{
                    try {
                        l.listen(line);
                    }catch (Exception ignored){}
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
                    startListeners.forEach(l->{
                        try {
                            l.listen(line);
                        }catch (Exception ignored){}
                    });
                }

            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private void parseErr(){
        try{
            BufferedReader stderr = new BufferedReader(new InputStreamReader(server.getErrorStream()));

            while(!Thread.interrupted() && server.isAlive()){
                String line = stderr.readLine();
                if (line == null) continue;

                errorListeners.forEach(l -> l.listen(line));

            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }



    public interface DataListener{
        void listen(String data);
    }
}
