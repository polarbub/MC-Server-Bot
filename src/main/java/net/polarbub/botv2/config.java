package net.polarbub.botv2;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

public class config {
    public static String pre;
    public static String token;
    public static URL webHookURL;

    public static long backupTime;
    public static long backupWarn;

    public static int port;
    public static String IP;
    public static String serverArgs;
    public static String serverDir;

    public static Pattern[] joinLeavePattern;
    public static Pattern chatBridgePattern;

    public static TextChannel chatBridgeChannel;
    public static TextChannel consoleChannel;
    public static MessageChannel returnChannel;

    public static YamlMapping permissionsConfig;

    public static void readConfig() throws IOException, InterruptedException, LoginException {
        YamlMapping config = Yaml.createYamlInput(new File("config.yaml")).readYamlMapping();
        YamlMapping backupConfig = config.yamlMapping("BACKUP");
        YamlMapping discordConfig = config.yamlMapping("DISCORD_BOT");
        YamlMapping minecraftConfig = config.yamlMapping("MC_SERVER");
        permissionsConfig = config.yamlMapping("PERMISSIONS");

        serverArgs = minecraftConfig.string("startCMD");
        port = minecraftConfig.integer("port");
        IP = minecraftConfig.string("ip");

        YamlSequence chatBridgeRegexSeq = discordConfig.yamlSequence("chat_regex");
        joinLeavePattern = new Pattern[chatBridgeRegexSeq.size()];
        for(int i = 0; i < chatBridgeRegexSeq.size(); i++) {
            joinLeavePattern[i] = Pattern.compile(chatBridgeRegexSeq.string(i));
        }
        chatBridgePattern = Pattern.compile(discordConfig.string("join&leave_regex"));
        //Get the channel IDs
        consoleChannel = Main.bot.getTextChannelById(discordConfig.longNumber("CONSOLE_CHANNEL"));
        chatBridgeChannel = Main.bot.getTextChannelById(discordConfig.longNumber("CHAT_CHANNEL"));
        webHookURL = new URL(discordConfig.string("chatBridgeWebHookURL"));
        token = discordConfig.string("TOKEN");
        pre = discordConfig.string("PREFIX");
        //init discord jda
        Main.bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(Main.bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000);

        backupTime = backupConfig.longNumber("backup_time");
        backupWarn = backupConfig.longNumber("backup_alert");
        serverDir = backupConfig.string("gitDirectory");
    }
}
