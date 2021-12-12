package net.polarbub.botv2.config;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.polarbub.botv2.Main;
import net.polarbub.botv2.message.say;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class config {
    public static String pre;
    public static String token;
    public static URL webHookURL;
    public static JDA bot;

    public static long backupTime;
    public static long backupWarn;

    public static int port;
    public static String IP;
    public static List<String> serverArgs = new ArrayList<>();
    public static String serverDir;

    public static List<normalPattern> normalPatterns = new ArrayList<>();
    public static List<namedPattern> namedPatterns = new ArrayList<>();

    public static Pattern startPattern;
    public static Pattern gitsavereturnPattern;

    public static TextChannel chatBridgeChannel;
    public static TextChannel consoleChannel;

    public static YamlMapping permissionsConfig;

    public static String test;

    public static void readConfig() throws IOException, InterruptedException, LoginException {
        YamlMapping config = Yaml.createYamlInput(new File(Main.runTimeArgs[0])).readYamlMapping();
        YamlMapping backupConfig = config.yamlMapping("BACKUP");
        YamlMapping discordConfig = config.yamlMapping("DISCORD_BOT");
        YamlMapping minecraftConfig = config.yamlMapping("MC_SERVER");
        permissionsConfig = config.yamlMapping("PERMISSIONS");

        serverArgs.clear();
        if (System.getProperty("os.name").equals("Linux")) {

            StringBuilder s = new StringBuilder();

            for(YamlNode node : minecraftConfig.yamlSequence("startCMD")) {
                s.append(node.asMapping().yamlMapping("entry").string("string"));
                s.append(" ");
            }

            s.delete(s.length() - 1,s.length());

            serverArgs.add(s.toString());

        } else {
            for(YamlNode node : minecraftConfig.yamlSequence("startCMD")) {
                serverArgs.add(node.asMapping().yamlMapping("entry").string("string"));
            }
        }

        webHookURL = new URL(discordConfig.string("chatBridgeWebHookURL"));

        //Get the channel IDs
        token = discordConfig.string("TOKEN");
        pre = discordConfig.string("PREFIX");
            //init discord jda
        bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES).addEventListeners(new Main()).build();
        while(!String.valueOf(bot.getStatus()).equals("CONNECTED")) { //wait for connected
            Thread.sleep(10);
        }
        Thread.sleep(1000);

        consoleChannel = bot.getTextChannelById(discordConfig.longNumber("CONSOLE_CHANNEL"));
        chatBridgeChannel = bot.getTextChannelById(discordConfig.longNumber("CHAT_CHANNEL"));

        test = discordConfig.string("test");

        //Add all normal regexs to list
        normalPatterns.clear();
        YamlSequence normalRegexes = minecraftConfig.yamlSequence("normalRegexes");

        for(YamlNode regexNode : normalRegexes) {
            net.polarbub.botv2.config.normalPattern NormalPattern = new normalPattern();
            YamlMapping regexSequence = regexNode.asMapping().yamlMapping("regex");
            NormalPattern.pattern = Pattern.compile(regexSequence.string("string"));
            NormalPattern.dataGroup = regexSequence.integer("contentGroup");
            normalPatterns.add(NormalPattern);
        }
        //Add all named regexs to list
        namedPatterns.clear();
        YamlSequence namedRegexs = minecraftConfig.yamlSequence("webHookRegexes");

        for(YamlNode regexNode : namedRegexs) {
            net.polarbub.botv2.config.namedPattern NamedPattern = new namedPattern();
            YamlMapping regexSequence = regexNode.asMapping().yamlMapping("regex");
            NamedPattern.pattern = Pattern.compile(regexSequence.string("string"));
            if(regexSequence.string("prefix").equals("\"\"")) {
                NamedPattern.prefix = "";
            } else {
                NamedPattern.prefix = regexSequence.string("prefix");
            }
            NamedPattern.dataGroup = regexSequence.integer("contentGroup");
            NamedPattern.nameGroup = regexSequence.integer("nameGroup");

            //System.out.println(regexSequence.integer("contentGroup"));
            //System.out.println(regexSequence.integer("nameGroup"));

            namedPatterns.add(NamedPattern);
        }

        startPattern = Pattern.compile(minecraftConfig.string("startRegex"));

        port = minecraftConfig.integer("port");
        IP = minecraftConfig.string("ip");

        backupTime = backupConfig.longNumber("backup_time");
        if(backupTime < 0) throw new IllegalArgumentException("Backup Time cannon be less than one");
        backupWarn = backupConfig.longNumber("backup_alert");
        serverDir = backupConfig.string("gitDirectory");

        gitsavereturnPattern = Pattern.compile(backupConfig.string("gitsavereturnRegex"));
    }
}

