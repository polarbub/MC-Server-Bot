const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordModule = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../Permissions.js');
const Discord = require('discord.js');

class HelpCommand extends Command {

    root : DiscordModule = null;

    constructor(root : DiscordModule) {
        super(root);
        this.root = root;
    }

    register() {
        this.root.commands['help'] = this;
    }

    unregister() {
        delete this.root.commands['help'];
    }

    execute(msg: Discord.Message, args: string) {
        if(args.length < 1) {
            let Embed = new Discord.MessageEmbed();

            Embed.setTitle("Available Commands:");
            Embed.setFooter("this are all the commands you're allowed to perform, please contact the administrator if any is missing");
            Object.entries(this.root.commands).filter((entry) => Permissions.isUserAllowed(msg, entry[0])
            ).forEach((entry : [string,Command]) => {
                Embed.addField(entry[0],entry[1].getDescription(),false)
            })
            msg.channel.send(Embed).catch(console.error);
        }else{
            let cmd = args.split(" ")[0];
            let cmdObj : Command = this.root.commands[cmd];
            if(cmdObj !== undefined && Permissions.isUserAllowed(msg,cmd)){
                let Embed = new Discord.MessageEmbed();
                Embed.setTitle("Help for '" + cmd + "'");
                Embed.setDescription(cmdObj.getHelp());
                msg.channel.send(Embed).catch(console.error);
            }
        }
    }

    getDescription() {
        return "shows this help page";
    }

    getHelp() {
        return "you really asked the help page of the help command?";
    }
}

module.exports = HelpCommand;