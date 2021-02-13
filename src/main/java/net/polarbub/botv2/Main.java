package net.polarbub.botv2;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;

public class Main extends ListenerAdapter {
    public static String pre = ".";
    public static ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx5G", "-Xms5G", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M", "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4", "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1", "fabric-server-launch.jar", "-nogui");

    public static void main(String[] args) throws LoginException {
        JDABuilder.createLight("Nzk2NDYyNTExMjkzMDcxMzYw.X_YRhA.OxCMJtcCuduaOqnQoIMQ7RWGnVI", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();

        if (msg.getContentRaw().equals(pre + "hi")) {
            System.out.println("hi");
            channel.sendMessageFormat("hi").queue();

        } else if (msg.getContentRaw().equals(pre + "stopbot")) {
            System.exit(0);

        } else if(msg.getContentRaw().equals(pre + "start")) {
            channel.sendMessageFormat("at some point this will start the server").queue();

        } else {
            Echobot.out(msg, event);
        }
        Echobot.sendhere = channel;
    }
}