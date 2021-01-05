const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Command = require('./Discord/Command.js');
const Permissions = require('../Permissions.js');
const Discord = require('discord.js');
const fs = require("fs");
const path = require('path');

class DiscordBot extends Module {

    main : Main = null;

    commandFiles = {}

    commands = {}

    onLoad() {
        if( this.main.Bot === undefined || this.main.DiscordModule === undefined ){
            this.main.DiscordModule = this;
            this.main.Bot = new Discord.Client();
            this.main.Bot.login(this.main.getConfigs().DISCORD_BOT.TOKEN).catch(console.error);
        }


        fs.readdir('./bin/scripts/modules/Discord/Commands',(err,files) => {
            if (err) {
                return console.log('Unable to scan Commands directory: ' + err);
            }

            files.forEach((file) => {
                if(path.extname(file) === ".js") {
                    file = path.resolve("./bin/scripts/modules/Discord/Commands") + "/" + file;
                    let command = require(file);
                    let instance: Command = new command(this);
                    instance.register(this.main.Bot);
                    this.commandFiles[file] = instance;
                }
            });
        })

        this.main.Bot.on('message', msg => {
            if(msg.author.bot)
                return;
            if(!msg.cleanContent.startsWith(this.main.getConfigs().DISCORD_BOT.PREFIX))
                return;
            if(!msg.channel.permissionsFor(msg.guild.me).has('SEND_MESSAGES'))
                return;
            let cmd = msg.cleanContent.substr(1).split(' ')[0];
            let command : Command = this.commands[cmd];
            if(command!==undefined){
                if(command.isAllowed(msg))
                    command.execute(msg,msg.cleanContent.substr(cmd.length+2))
            }
        })
    }

    onUnload() {
        Object.keys(this.commandFiles).forEach((key : string)=> {
            let command : Command = this.commandFiles[key];
            command.unregister(this.main.Bot);
            delete require.cache[key];
        });
    }

    getBot(): Discord.Client{
        return this.main.Bot;
    }

    constructor(main : Main){
        super(main);
        this.main = main;
    }

}

module.exports = DiscordBot;