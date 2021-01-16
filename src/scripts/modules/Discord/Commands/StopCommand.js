const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const MinecraftServer = require('../../MinecraftModule.js');


class StopCommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        (this.root : DiscordBot) = module;
    }

    register() {
        (this.root : DiscordBot).commands['stop'] = this;
        Permissions.addPermission('commands.stop');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['stop'];
    }

    execute(msg, args) {
        let mcModule : MinecraftServer = (this.root : DiscordBot).main['MinecraftServer'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        Embed.setDescription("Stopping the server");
        Embed.setColor(Colors.DARK_GREEN);
        (msg : Discord.Message).channel.send(Embed).catch(console.error);
        Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if ((mcModule : MinecraftServer).getServer() !== null) {

            (this.root : DiscordBot).getBot().user.setActivity("Server SHUTDOWN", {type: "WATCHING"}).catch(console.error);
            (mcModule : MinecraftServer).getServer().on('exit', () => {
                Embed.setDescription("Server Stopped");
                Embed.setColor(Colors.GREEN);
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
            });

            (mcModule : MinecraftServer).stop();

        } else {
            Embed.setDescription("Server already stopped");
            Embed.setColor(Colors.BLUE);
            (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "stops the Minecraft Server";
    }

    getHelp() : Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `stop`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.stop');
    }
}

module.exports = StopCommand;
