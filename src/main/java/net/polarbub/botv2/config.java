package net.polarbub.botv2;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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
    public static int gitPushOn;
    public static String gitPushOptions;

    public static List<normalPattern> normalPatterns;
    public static List<namedPattern> namedPatterns;

    public static TextChannel chatBridgeChannel;
    public static TextChannel consoleChannel;

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

        //Get the channel IDs
        token = discordConfig.string("TOKEN");
        pre = discordConfig.string("PREFIX");
        //init discord jda
        Main.bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(Main.bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000);

        consoleChannel = Main.bot.getTextChannelById(discordConfig.longNumber("CONSOLE_CHANNEL"));
        chatBridgeChannel = Main.bot.getTextChannelById(discordConfig.longNumber("CHAT_CHANNEL"));

        //Add all normal regexs to list
        YamlSequence normalRegexes = discordConfig.yamlSequence("normalRegexes");
        for(YamlNode regexNode : normalRegexes) {
            normalPattern NormalPattern = new normalPattern();
            YamlMapping regexSequence = regexNode.asMapping();
            NormalPattern.pattern = Pattern.compile(regexSequence.string("string"));
            NormalPattern.dataGroup = regexSequence.integer("contentGroup");
            normalPatterns.add(NormalPattern);
        }

        if(!discordConfig.string("chatBridgeWebHookURL").isEmpty()) {
            webHookURL = new URL(discordConfig.string("chatBridgeWebHookURL"));
            //Add all named regexs to list
            YamlSequence namedRegexs = discordConfig.yamlSequence("webHookRegexes");
            for(YamlNode regexNode : namedRegexs) {
                namedPattern NamedPattern = new namedPattern();
                YamlMapping regexSequence = regexNode.asMapping();
                NamedPattern.pattern = Pattern.compile(regexSequence.string("string"));
                NamedPattern.dataGroup = regexSequence.integer("contentGroup");
                NamedPattern.nameGroup = regexSequence.integer("nameGroup");
                namedPatterns.add(NamedPattern);
            }
        }

        backupTime = backupConfig.longNumber("backup_time");
        backupWarn = backupConfig.longNumber("backup_alert");
        serverDir = backupConfig.string("gitDirectory");
        gitPushOn = backupConfig.integer("on");
        gitPushOptions = backupConfig.string("options");
    }
}

class normalPattern {
    public Pattern pattern;
    public int dataGroup;
}

class namedPattern extends normalPattern {
    public int nameGroup;
}