const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Command = require('./Discord/Command.js');
const Permissions = require('../Permissions.js');
const Discord = require('discord.js');
const fs = require("fs");
const path = require('path');

let chatRegex = null;

class ConsoleChannelModule extends Module {

    main: Main = null;

    channel: Discord.TextChannel = null;

    getBot(): Discord.Client {
        return this.main['Bot'];
    }

    constructor(main) {
        super(main);
        this.main = main;
    }

    buffer: string = "";

    listeners = {};

    onLoad() {
        this.main["ChatChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {
            this.listeners['ready'] = this.getBot().on('ready', () => {
                this.getBot().channels.fetch(this.main.getConfigs().DISCORD_BOT.CHAT_CHANNEL).then(
                    (channel) => {
                        this.channel = channel;

                        this.listeners['start'] = this.main['MinecraftServer'].on('start', (instance) => {
                            this.listeners['data'] = instance.stdout.on('data', (data) => {
                                if(chatRegex===null)
                                    chatRegex = new RegExp(this.main.getConfigs().MC_SERVER.chat_regex,'gm');
                                let res = chatRegex.exec(data);
                                if(res!=null)
                                    this.channel.send(res[0].replace(/([\\*`'_~])/gm, "\\$&"), {split: true}).catch(console.error);
                            })
                        })

                        this.listeners['message'] = this.getBot().on('message',(msg)=>{
                            if(msg.author.bot)
                                return;
                            if(msg.channel.id !== this.channel.id)
                                return;
                            if(msg.channel.permissionsFor(msg.guild.me).has('SEND_MESSAGES', 'VIEW_CHANNEL')){
                                this.main['DiscordModule'].commands['say'].execute(msg,msg.cleanContent);
                            }
                        })
                    }
                ).catch(console.error);

            });
        });
    }

    onUnload() {
        let listener = this.listeners['ready'];
        if(listener!==undefined)
            this.getBot().removeListener('ready',listener);
        listener = this.listeners['start'];
        if(listener!==undefined)
            this.main['MinecraftServer'].removeListener('start',listener);
        listener = this.listeners['data'];
        if(listener!==undefined)
            this.main['server'].removeListener('data',listener);
        listener = this.listeners['message'];
        if(listener!==undefined)
            this.getBot().removeListener('message',listener);
    }
}

module.exports = ConsoleChannelModule;