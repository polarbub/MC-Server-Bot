const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const MinecraftServer = require('../../MinecraftModule.js');


class ExecCommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        (this.root : DiscordBot) = ( module : DiscordBot );
    }

    register() {
        ((this.root : DiscordBot) : DiscordBot).commands['exec'] = this;
        Permissions.addPermission('commands.exec');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['exec'];
    }

    execute(msg : Discord.Message, args) {
        let consoleChannel : Discord.TextChannel = (this.root : DiscordBot).main['ConsoleChannel'].channel;
        let mcModule : MinecraftServer = (this.root : DiscordBot).main['MinecraftServer'] ;
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if((mcModule : MinecraftServer).getServer() !== null) {
            if (args.trim().length > 1) {
                Embed.addField("Command:", args);
                (mcModule : MinecraftServer).exec(args).then((res : string) => {
                    if(res.length > 0) {
                        Embed.setDescription(res.substr(0, 2000));
                        if ((msg : Discord.Message).channel.id !== (consoleChannel : Discord.TextChannel)?.id)
                            (msg : Discord.Message).channel.send(Embed).catch(console.error);
                    }
                }).catch(console.error);
            } else {
                Embed.setDescription("No command Specified");
                Embed.setColor(Colors.RED);
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor(Colors.BLUE);
            (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "executes commands on the game console";
    }

    getHelp(): Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `exec`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg : Discord.Message) {
        return Permissions.isUserAllowed((msg : Discord.Message), 'commands.exec');
    }
}

module.exports = ExecCommand;
