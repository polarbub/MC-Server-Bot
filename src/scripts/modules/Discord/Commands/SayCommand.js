const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;
//const NTC = require('../../../../libs/ntc.js').ntc;

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
                //let color = (member!==undefined)?NTC.name((msg : Discord.Message).member.displayHexColor)[3].toLowerCase():"aqua"
                let color = "aqua";

                let msgJSON = [""];
                msgJSON.push({
                    text:"[DISCORD]",
                    color:"dark_blue",
                    hoverEvent: {
                        action:"show_text",
                        contents:[
                            {
                                text:"Open on Discord",
                                italic: true,
                                underlined: true,
                                color:"blue"
                            }
                        ]
                    },
                    clickEvent:{
                        action:"open_url",
                        value: (msg : Discord.Message).url
                    }
                });
                msgJSON.push({
                    text:" <"
                });
                msgJSON.push({
                    text:username,
                    color:color,
                    hoverEvent: {
                        action:"show_text",
                        contents: [
                            {
                                text: msg.author.username + "#" + msg.author.discriminator,
                                color: "blue",
                                underlined: true
                            }
                        ]
                    }
                });
                msgJSON.push({
                    text:"> "
                });

                if(msg.attachments.size>0){
                    msg.attachments.forEach((file)=>{
                        msgJSON.push({
                            text:"[",
                            extra:
                            [
                                {
                                    text:"File",
                                    underlined:true
                                },
                                {
                                    text:"] "
                                }
                            ],
                            color: "blue",
                            hoverEvent: {
                                action:"show_text",
                                contents: [
                                    {
                                        text: "Open File in Browser",
                                        color: "blue",
                                        underlined: true
                                    }
                                ]
                            },
                            clickEvent: {
                                action:"open_url",
                                value: file.url
                            }
                        })
                    })
                }

                msgJSON.push({
                    text:args
                });


                let command = (this.root : DiscordBot).main.getConfigs()["MC_SERVER"]['say_format'];
                command = command.replace("%username%",username);
                command = command.replace("%color%",color);
                command = command.replace("%message%",JSON.stringify(args).slice(1,-1));
                command = command.replace("%messageJSON%",JSON.stringify(msgJSON));
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
