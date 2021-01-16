const DiscordModule = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

class HelpCommand extends Command {

    root : DiscordModule = null;

    constructor(root) {
        super(root);
        (this.root : DiscordModule) = root;
    }

    register() {
        (this.root : DiscordModule).commands['help'] = this;
        Permissions.addPermission("commands.help");
    }

    unregister() {
        delete (this.root : DiscordModule).commands['help'];
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg,"commands.help");
    }

    execute(msg, args) {
        if(args.length < 1) {
            let Embed = new Discord.MessageEmbed();

            Embed.setTitle("Available Commands:");
            Embed.setFooter("this are all the commands you're allowed to perform, please contact the administrator if any is missing");
            Object.entries((this.root : DiscordModule).commands).filter((entry) => entry[1].isAllowed(msg)
            ).forEach((entry) => {
                Embed.addField(entry[0],entry[1].getDescription(),false)
            });
            (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }else{
            let cmd = args.split(" ")[0];
            let cmdObj = (this.root : DiscordModule).commands[cmd];
            if(cmdObj !== undefined && cmdObj.isAllowed(msg)){
                let Embed = cmdObj.getHelp();
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
            }
        }
    }

    getDescription() {
        return "shows this help page";
    }

    getHelp(): Discord.MessageEmbed {
        let embed = new Discord.MessageEmbed();
        embed.setDescription("you really requested the help for the help command?")
        return embed;
    }
}

module.exports = HelpCommand;