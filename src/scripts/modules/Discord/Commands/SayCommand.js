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
        (this.root : DiscordBot) = module;
    }

    register() {
        (this.root : DiscordBot).commands['say'] = this;
        Permissions.addPermission('commands.say');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['say'];
    }

    execute(msg, args) {
        let mcModule : MinecraftServer= (this.root : DiscordBot).main['MinecraftServer'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if((mcModule : MinecraftServer).getServer() !== null) {
            if (args.trim().length > 0) {
                let member = (msg : Discord.Message).member;
                let username = (member!==undefined)?(msg : Discord.Message).member.displayName:(msg : Discord.Message).author.username + "#" + (msg : Discord.Message).author.discriminator;
                //let color = (member!==undefined)?(msg : Discord.Message).member.displayHexColor.substr(0,7):"aqua"; not yet working so only aqua colors for now
                let color = "aqua";

                let command = (this.root : DiscordBot).main.getConfigs()["MC_SERVER"]['say_format'];
                command = command.replace("%username%",username);
                command = command.replace("%color%",color);
                command = command.replace("%message%",JSON.stringify(args).slice(1,-1));
                command = command.replace("%messageJSON%",JSON.stringify({text:args}));
                (mcModule : MinecraftServer).exec(command)

                Embed.setDescription("sent");
                if((this.root : DiscordBot).main['ChatChannel'] === undefined || (this.root : DiscordBot).main['ChatChannel'].channel?.id !== (msg : Discord.Message).channel.id)
                    (msg : Discord.Message).channel.send(Embed).catch(console.error);
            } else {
                Embed.setDescription("No text Specified");
                Embed.setColor(Colors.RED);
                if((this.root : DiscordBot).main['ChatChannel'] === undefined || (this.root : DiscordBot).main['ChatChannel'].channel?.id !== (msg : Discord.Message).channel.id)
                    (msg : Discord.Message).channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor(Colors.BLUE);
            if((this.root : DiscordBot).main['ChatChannel'] === undefined || (this.root : DiscordBot).main['ChatChannel'].channel?.id !== (msg : Discord.Message).channel.id)
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "sends a chat message to the server";
    }

    getHelp() : Discord.MessageEmbed {
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
