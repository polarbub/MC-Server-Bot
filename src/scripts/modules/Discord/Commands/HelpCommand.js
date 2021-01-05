const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordModule = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

class HelpCommand extends Command {

    root : DiscordModule = null;

    constructor(root : DiscordModule) {
        super(root);
        this.root = root;
    }

    register() {
        this.root.commands['help'] = this;
        Permissions.addPermission("commands.help");
    }

    unregister() {
        delete this.root.commands['help'];
    }

    isAllowed(msg: Discord.Message) {
        return Permissions.isUserAllowed(msg,"commands.help");
    }

    execute(msg: Discord.Message, args: string) {
        if(args.length < 1) {
            let Embed = new Discord.MessageEmbed();

            Embed.setTitle("Available Commands:");
            Embed.setFooter("this are all the commands you're allowed to perform, please contact the administrator if any is missing");
            Object.entries(this.root.commands).filter((entry : [string,Command]) => entry[1].isAllowed(msg)
            ).forEach((entry : [string,Command]) => {
                Embed.addField(entry[0],entry[1].getDescription(),false)
            })
            msg.channel.send(Embed).catch(console.error);
        }else{
            let cmd = args.split(" ")[0];
            let cmdObj : Command = this.root.commands[cmd];
            if(cmdObj !== undefined && cmdObj.isAllowed(msg)){
                let Embed = cmdObj.getHelp();
                msg.channel.send(Embed).catch(console.error);
            }
        }
    }

    getDescription() {
        return "shows this help page";
    }

    getHelp() {
        let embed = new Discord.MessageEmbed();
        embed.setDescription("You really requested the help page of the help command?")
        return embed;
    }
}

module.exports = HelpCommand;