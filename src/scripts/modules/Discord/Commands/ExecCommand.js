const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

const MinecraftServer = require('../../MinecraftModule.js');


class ExecCommand extends Command {

    root;

    constructor(module) {
        super(module);
        this.root = module;
    }

    register() {
        this.root.commands['exec'] = this;
        Permissions.addPermission('commands.exec');
    }

    unregister() {
        delete this.root.commands['exec'];
    }

    execute(msg, args) {
        let consoleChannel : Discord.TextChannel = this.root.main['ConsoleChannel'].channel;
        let mcModule = this.root.main.MinecraftServer;
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if(mcModule.getServer() !== null) {
            if (args.trim().length > 1) {
                Embed.addField("Command:", args);
                mcModule.exec(args, (res) => {
                    Embed.setDescription(res.substr(0,2000));
                    if(msg.channel.id !== consoleChannel.id)
                        msg.channel.send(Embed).catch(console.error);
                })
            } else {
                Embed.setDescription("No command Specified");
                Embed.setColor("#FF0000");
                msg.channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor("#FF0000");
            msg.channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "executes commands on the game console";
    }

    getHelp() {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `exec`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.exec');
    }
}

module.exports = ExecCommand;
