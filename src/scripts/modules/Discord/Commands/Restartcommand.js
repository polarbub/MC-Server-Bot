const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const MinecraftServer = require('../../MinecraftModule.js');

let periodicCheck;


class Restartcommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        this.root = module;
    }

    register() {
        this.root.commands['restart'] = this;
        Permissions.addPermission('commands.restart');
    }

    unregister() {
        delete this.root.commands['restart'];
    }

    execute(msg, args) {
        let mcModule : MinecraftServer = this.root.main['MinecraftServer'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        Embed.setDescription("Restarting the server");
        Embed.setColor(Colors.DARK_GREEN);
        msg.channel.send(Embed).catch(console.error);
        Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if (mcModule.getServer() !== null) {

            this.root.getBot().user.setActivity("Server SHUTDOWN", {type: "WATCHING"}).catch(console.error);
            mcModule.getServer().on('exit', () => {
                Embed.setDescription("Server Stopped");
                Embed.setColor(Colors.GREEN);
                msg.channel.send(Embed).catch(console.error);

                Embed = new Discord.MessageEmbed();
                Embed.setTitle("MC Server");
                Embed.setDescription("Starting the Server");
                Embed.setColor(Colors.DARK_GREEN);
                msg.channel.send(Embed).catch(console.error);
                Embed = new Discord.MessageEmbed();
                Embed.setTitle("MC Server");

                mcModule.start();

                if (mcModule.getServer().exitCode === null) {
                    this.root.getBot().user.setActivity("Server Startup", {type: "WATCHING"}).catch(console.error);
                    let doneHandler = (chunk) => {
                        if (chunk.includes("Done")) {
                            Embed.setDescription("Server Started");
                            Embed.setColor(Colors.GREEN);
                            msg.channel.send(Embed).catch(console.error);
                            mcModule.getServer().stdout.removeListener('data', doneHandler);
                            this.root.getBot().user.setActivity("Players on the Server", {type: "WATCHING"}).catch(console.error);

                            periodicCheck = setInterval(() => {
                                mcModule.status((res) => {
                                    this.root.getBot().user.setActivity(res.onlinePlayers + " Players on the Server", {type: "WATCHING"}).catch(console.error);
                                })
                            }, 30000)
                        }
                    }
                    mcModule.getServer().stdout.on('data', doneHandler);
                    mcModule.getServer().on('exit', () => {
                        this.root.getBot().user.setActivity("Commands", {type: "LISTENING"}).catch(console.error);
                        clearInterval(periodicCheck);
                    })
                } else {
                    Embed.setDescription("Something went wrong");
                    Embed.setColor(Colors.RED);
                    msg.channel.send(Embed).catch(console.error);
                }
            })

            mcModule.stop();

        } else {
            Embed.setDescription("Server is stopped");
            Embed.setColor(Colors.BLUE);
            msg.channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "stops the Minecraft Server";
    }

    getHelp() {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `restart`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.restart');
    }
}

module.exports = Restartcommand;
