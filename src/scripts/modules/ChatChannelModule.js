const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Discord = require('discord.js');

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

    listeners = {};

    mcServer;

    onLoad() {
        this.main["ChatChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {
            this.getBot().on('ready', this.listeners['ready'] = () => {
                this.getBot().channels.fetch(this.main.getConfigs()['DISCORD_BOT']['CHAT_CHANNEL']).then(
                    (channel) => {
                        this.channel = channel;

                        this.mcServer = this.main['MinecraftServer'];
                        this.setStartListener();

                        this.getBot().on('message',this.listeners['message'] = (msg)=>{
                            if(msg.author.bot)
                                return;
                            if(msg.channel.id !== this.channel?.id)
                                return;
                            if(msg.guild === undefined || msg.guild === null)
                                return;
                            if(msg.channel.permissionsFor(msg.guild.me).has('SEND_MESSAGES', 'VIEW_CHANNEL')){
                                this.main['DiscordModule'].commands['say'].execute(msg,msg.cleanContent);
                            }
                        })

                        this.main.on('reload',this.listeners['reload'] = (old_module,new_module)=>{
                            if(old_module === this.mcServer){
                                this.mcServer = new_module;
                                new_module.on('start',this.listeners['start']);
                            }
                        })
                    }
                ).catch(console.error);

            });
        });
    }

    setStartListener() {
        this.mcServer.on('start', this.listeners['start'] = (instance) => {
            instance.stdout.on('data', this.listeners['data'] = (data) => {
                if (chatRegex === null)
                    chatRegex = new RegExp(this.main.getConfigs()['MC_SERVER']['chat_regex'], 'gm');
                let res = [ ...data.matchAll(chatRegex)];
                res.forEach((match)=>{
                    this.channel.send(match[1].replace(/([\\*`'_~])/gm, "\\$&"), {split: true}).catch(console.error);
                })
            })
        })
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
        listener = this.listeners['reload'];
        if(listener!==undefined)
            this.main.removeListener('reload',listener);
    }
}

module.exports = ConsoleChannelModule;