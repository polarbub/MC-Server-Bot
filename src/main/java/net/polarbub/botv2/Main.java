package net.polarbub.botv2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import javax.security.auth.login.LoginException;
import java.io.*;


public class Main extends ListenerAdapter {
    public static String pre = ".";
    public static ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx5G", "-Xms5G", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M", "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4", "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1", "fabric-server-launch.jar", "-nogui");
    public static TextChannel ConsoleChannel;
    public static MessageChannel ReturnChannel;
    public static Process p;
    public static BufferedWriter bw;
    public static BufferedReader br;
    public static boolean serverrunning = false;


    public static void main(String[] args) throws LoginException, InterruptedException{
        //inti discord jda
        JDA bot = JDABuilder.createLight("token", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }

        Thread.sleep(1000);

        ConsoleChannel = bot.getTextChannelById("796517469224960072");
        ReturnChannel = ConsoleChannel;
        pb.directory(new File("E:\\saves\\.minecraft\\server\\protosky-testing"));
        pb.redirectErrorStream(true);

        //start the console in a thread
        in.main();

    }
    //message processing
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Message msg = event.getMessage();
            ReturnChannel = event.getChannel();

            if (msg.getContentRaw().equals(pre + "init")) {
                System.out.println("done");
                ReturnChannel.sendMessageFormat("done").queue();

            } else if (msg.getContentRaw().equals(pre + "stopbot")) {
                System.exit(0);

            } else if (msg.getContentRaw().equals(pre + "stop")) {
                //ReturnChannel.sendMessageFormat("at some point this will start the server").queue();
                p.destroy();
                ReturnChannel.sendMessageFormat("stopped server").queue();

            } else if (msg.getContentRaw().equals(pre + "start")) {
                if(serverrunning) {
                    ReturnChannel.sendMessageFormat("Server is Running rn").queue();
                } else {
                    serverrunning = true;
                    server.main();
                }

            } else {
                if(in.tosay.equals(msg.getContentRaw())){
                    int a = 0;
                } else {
                    System.out.println("Author: " + msg.getAuthor() + " Server: " + event.getGuild() + " Channel: " + msg.getChannel());
                    System.out.println("Content: " + msg.getContentRaw());
                }

                if (String.valueOf(ReturnChannel).equals(String.valueOf(ConsoleChannel)) && serverrunning) {
                    try {
                        Main.bw.write(msg.getContentRaw());
                        Main.bw.newLine();
                        Main.bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}