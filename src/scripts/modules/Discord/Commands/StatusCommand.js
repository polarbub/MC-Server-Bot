const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');


class StartCommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        (this.root : DiscordBot) = module;
    }

    register() {
        (this.root : DiscordBot).commands['status'] = this;
        Permissions.addPermission('commands.status');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['status'];
    }

    execute(msg, args) {
        let mcModule = (this.root : DiscordBot).main['MinecraftServer'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("MC Server");
        if (mcModule.getServer() !== null) {
            mcModule.status().then((response)=>{
                if(response.favicon !== undefined && response.favicon !== null) {
                    let fav = response.favicon.split(",").slice(1).join(",");
                    let imageStream = Buffer.from(fav, "base64");
                    let attachment = new Discord.MessageAttachment(imageStream, "favicon.png");
                    Embed.attachFiles([attachment])
                    Embed.setThumbnail("attachment://favicon.png");
                }
                Embed.setDescription("Server is Up")
                Embed.addField("hostname",response.host, true);
                Embed.addField("port",response.port,true);
                Embed.addField("version",response.version, true);
                Embed.addField("players","there are " +response.onlinePlayers + " over " + response.maxPlayers + " max");
                if(response.samplePlayers != null){
                    Embed.addField("List:",response.samplePlayers.map(p=>p.name).join(", "));
                }
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
            }).catch(console.error);
        } else {
            Embed.setDescription("Server not running");
            Embed.setColor('#0018f1');
            (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "get info about the Minecraft Server";
    }

    getHelp() : Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `status`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.status');
    }
}

module.exports = StartCommand;
