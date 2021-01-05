const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

const MinecraftServer = require('../../MinecraftModule.js');

let periodicCheck;

class StartCommand extends Command {

    root: DiscordBot;

    constructor(module: DiscordBot) {
        super(module);
        this.root = module;
    }

    register() {
        this.root.commands['start'] = this;
        Permissions.addPermission('commands.start');
    }

    unregister() {
        delete this.root.commands['start'];
    }

    execute(msg: Discord.Message, args: string) {
        let mcModule: MinecraftServer = this.root.main.MinecraftServer;
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        Embed.setDescription("Starting the Server");
        Embed.setColor('#51e879');
        msg.channel.send(Embed).catch(console.error);
        Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if (mcModule.getServer() === null) {

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
                            Mc.status(config, Server, (res) => {
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
        } else {
            Embed.setDescription("Server already running");
            Embed.setColor('#0018f1');
            msg.channel.send(Embed).catch(console.error);
        }
    }

    getDescription(): string {
        return "start the Minecraft Server";
    }

    getHelp(): Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `start`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg: Discord.Message): boolean {
        return Permissions.isUserAllowed(msg, 'commands.start');
    }
}

module.exports = StartCommand;
