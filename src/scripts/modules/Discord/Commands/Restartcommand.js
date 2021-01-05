const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

const MinecraftServer = require('../../MinecraftModule.js');

let periodicCheck;


class Restartcommand extends Command {

    root;

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
        let mcModule = this.root.main.MinecraftServer;
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        Embed.setDescription("Restarting the server");
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

                Embed = new Discord.MessageEmbed();
                Embed.setTitle("MC Server");
                Embed.setDescription("Starting the Server");
                Embed.setColor('#51e879');
                msg.channel.send(Embed).catch(console.error);
                Embed = new Discord.MessageEmbed();
                Embed.setTitle("MC Server");

                mcModule.start();

                if (mcModule.getServer().exitCode === null) {
                    this.root.getBot().user.setActivity("Server Startup", {type: "WATCHING"});
                    let doneHandler = (chunk) => {
                        if (chunk.includes("Done")) {
                            Embed.setDescription("Server Started");
                            Embed.setColor('#099a02');
                            msg.channel.send(Embed).catch(console.error);
                            mcModule.getServer().stdout.removeListener('data', doneHandler);
                            this.root.getBot().user.setActivity("Players on the Server", {type: "WATCHING"});

                            periodicCheck = setInterval(() => {
                                mcModule.status((res) => {
                                    this.root.getBot().user.setActivity(res.onlinePlayers + " Players on the Server", {type: "WATCHING"});
                                })
                            }, 30000)
                        }
                    }
                    mcModule.getServer().stdout.on('data', doneHandler);
                    mcModule.getServer().on('exit', () => {
                        this.root.getBot().user.setActivity("Commands", {type: "LISTENING"});
                        clearInterval(periodicCheck);
                    })
                } else {
                    Embed.setDescription("Something went wrong");
                    Embed.setColor('#f10000');
                    msg.channel.send(Embed).catch(console.error);
                }
            })

            mcModule.stop();

        } else {
            Embed.setDescription("Server is stopped");
            Embed.setColor('#0018f1');
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
