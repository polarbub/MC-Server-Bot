package com.mattymatty.mcbot;

public class Config {

    public Bot DISCORD_BOT;
    public Server MC_SERVER;
    public Backup BACKUP;

    public static class Bot{
        public String token;
        public String chat_channel_marker;
        public String console_channel_marker;
        public String command_channel_marker;
        public String console_command_channel_marker;
        public String command_marker_mode;
        public Long[] guilds;
    }

    public static class Server{
        public String startCMD;
        public String ip;
        public String say_format;
        public String[] chat_regex;
        public String save_regex;
        public String done_regex;
        public String error_regex;
    }

    public static class Backup{
        public String server_path;
        public Integer backup_time;
        public Integer backup_alert;
    }
}
