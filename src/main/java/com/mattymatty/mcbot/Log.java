package com.mattymatty.mcbot;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class Log {
    private Queue<String> logQueue = new ConcurrentLinkedQueue<>();
    private Semaphore logSem = new Semaphore(0);
    private Set<DataListener> logListeners = new LinkedHashSet<>();

    public Log() {
        new Thread(()->{
            while (!Thread.interrupted()){
                try{
                    logSem.acquire();
                    String log = logQueue.poll();
                    logListeners.forEach(l -> {
                        try{
                            l.listen(log);
                        }catch (Exception ignored){}
                    });
                }catch (InterruptedException ignored){}
            }
        },"Log thread").start();
    }

    public void addLogListener(DataListener listener){
        logListeners.add(listener);
    }

    public void removeLogListener(DataListener listener){
        logListeners.remove(listener);
    }

    public void print(String string){
        if(string!=null)
            logQueue.add(string);
    }
}
