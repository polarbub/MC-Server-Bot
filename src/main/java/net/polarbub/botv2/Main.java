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

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        //inti discord jda
        JDA bot = JDABuilder.createLight("Nzk2NDYyNTExMjkzMDcxMzYw.X_YRhA.mAISK0dNEE34pidscAR8Oeh0yk8", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000); //init time

        //define some vars
        ConsoleChannel = bot.getTextChannelById("796517469224960072");
        ReturnChannel = ConsoleChannel;
        //debug code
        /*System.out.println(ConsoleChannel);
        System.out.println(ReturnChannel);*/

        //start the console in a thread
        in.main();

        //server testing
        pb.directory(new File("E:\\saves\\.minecraft\\server\\protosky-testing"));
        pb.redirectErrorStream(true);
        p = pb.start();
        InputStream is = p.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            System.out.println( line ); // Or just ignore it
            //ConsoleChannel.sendMessageFormat(line).queue();
        }
        p.waitFor();
        System.out.println("server started");

    }
    //message processing
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        ReturnChannel = event.getChannel();
        if (msg.getContentRaw().equals(pre + "init")) {
            System.out.println("done");
            ReturnChannel.sendMessageFormat("done").queue();

        } else if (msg.getContentRaw().equals(pre + "stopbot")) {
            System.exit(0);

        } else if(msg.getContentRaw().equals(pre + "stop")) {
            //ReturnChannel.sendMessageFormat("at some point this will start the server").queue();
            System.out.println("before kill");
            p.destroy();
            System.out.println("after kill");
            ReturnChannel.sendMessageFormat("stopped server").queue();

        } else if(!event.getAuthor().isBot()) {
            out.output(msg, event); //print the all messages non-command messages
        }
    }
}