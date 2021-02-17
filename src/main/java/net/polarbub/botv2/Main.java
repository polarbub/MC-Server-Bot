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


public class Main extends ListenerAdapter {
    public static String pre = ".";
    public static ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx5G", "-Xms5G", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M", "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4", "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1", "fabric-server-launch.jar", "-nogui");
    public static TextChannel ConsoleChannel;
    public static MessageChannel ReturnChannel;

    public static void main(String[] args) throws LoginException, InterruptedException {
        //inti discord jda
        JDA bot = JDABuilder.createLight("token", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(100); //init time

        //refine some vars
        ConsoleChannel = bot.getTextChannelById("796517469224960072");
        ConsoleChannel = (TextChannel) ReturnChannel;
        //start the console in thread
        in.main();

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

        } else if(msg.getContentRaw().equals(pre + "start")) {
            ReturnChannel.sendMessageFormat("at some point this will start the server").queue();

        } else if(!event.getAuthor().isBot()) {
            out.output(msg, event); //print the all messages non-command messages
        }
    }
}