const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

const MinecraftServer = require('../../MinecraftModule.js');


class StopCommand extends Command {

    root;

    constructor(module) {
        super(module);
        this.root = module;
    }

    register() {
        this.root.commands['stop'] = this;
        Permissions.addPermission('commands.stop');
    }

    unregister() {
        delete this.root.commands['stop'];
    }

    execute(msg, args) {
        let mcModule = this.root.main.MinecraftServer;
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        Embed.setDescription("Stopping the server");
        Embed.setColor('#51e879');
        msg.channel.send(Embed).catch(console.error);
        Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if (mcModule.getServer() !== null) {

            this.root.getBot().user.setActivity("Server SHUTDOWN", {type: "WATCHING"});
            mcModule.getServer().on('exit', () => {
                Embed.setDescription("Server Stopped");
                Embed.setColor("#0032f3");
                msg.channel.send(Embed).catch(console.error);
            })

            mcModule.stop();

        } else {
            Embed.setDescription("Server already stopped");
            Embed.setColor('#0018f1');
            msg.channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "stops the Minecraft Server";
    }

    getHelp() {
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
