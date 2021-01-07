const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const MinecraftServer = require('../../MinecraftModule.js');


class SayCommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        this.root = module;
    }

    register() {
        this.root.commands['say'] = this;
        Permissions.addPermission('commands.say');
    }

    unregister() {
        delete this.root.commands['say'];
    }

    execute(msg, args) {
        let mcModule : MinecraftServer= this.root.main['MinecraftServer'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if(mcModule.getServer() !== null) {
            if (args.trim().length > 1) {
                let member = msg.member;
                let username = (member!==undefined)?msg.member.displayName:msg.author.username + "#" + msg.author.discriminator;
                //let color = (member!==undefined)?msg.member.displayHexColor.substr(0,7):"acqua"; not yet working so only aqua colors for now
                let color = "aqua";

                let command = this.root.main.getConfigs()["MC_SERVER"]['say_format'];
                command = command.replace("%username%",username);
                command = command.replace("%color%",color);
                command = command.replace("%message%",args);
                mcModule.exec(command)

                Embed.setDescription("sent");
                if(this.root.main['ChatChannel'] === undefined || this.root.main['ChatChannel'].channel?.id !== msg.channel.id)
                    msg.channel.send(Embed).catch(console.error);
            } else {
                Embed.setDescription("No text Specified");
                Embed.setColor(Colors.RED);
                if(this.root.main['ChatChannel'] === undefined || this.root.main['ChatChannel'].channel?.id !== msg.channel.id)
                    msg.channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor(Colors.BLUE);
            if(this.root.main['ChatChannel'] === undefined || this.root.main['ChatChannel'].channel?.id !== msg.channel.id)
                msg.channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "sends a chat message to the server";
    }

    getHelp() {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `say`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.say');
    }
}

module.exports = SayCommand;
