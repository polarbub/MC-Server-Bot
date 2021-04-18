const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Discord = require('discord.js');

const ipRegex = /\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/gm;

class ConsoleChannelModule extends Module {

    main: Main = null;

    channel: Discord.TextChannel = null;

    getBot(): Discord.Client {
        return (this.main : Main)['Bot'];
    }

    constructor(main) {
        super(main);
        (this.main : Main) = main;
    }

    discordLogger = null;
    buffer: string = "";

    listeners = {};

    onLoad() {
        (this.main : Main)["ConsoleChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {

            this.getBot().on('ready', this.listeners['ready'] = () => {
                this.getBot().channels.fetch((this.main : Main).getConfigs()['DISCORD_BOT']['CONSOLE_CHANNEL']).then(
                    (channel) => {
                        this.channel = channel;

                        this.discordLogger = setInterval(() => {
                            if (this.channel != null && this.buffer !== "") {
                                let messages = this.main["DiscordModule"].splitMessage(this.buffer.replace(ipRegex, "||CENSORED IP||").replace(/([\\*`'_~])/gm, "\\$&"));
                                for(let msg of messages)
                                    this.channel.send(msg).catch(console.error);
                                this.buffer = "";
                            }
                        }, 500);

                        (this.main : Main)['MinecraftServer'].on('start', this.listeners['start'] = (instance) => {
                             instance.stdout.on('data', this.listeners['data'] = (data) => {
                                this.buffer += data;
                            })
                        });

                        (this.main : Main).on('reload',this.listeners['reload'] = (old_module,new_module)=>{
                            if(old_module === this.mcServer){
                                this.mcServer = new_module;
                                new_module.on('start',this.listeners['start']);
                            }
                        });

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
            (this.main : Main)['MinecraftServer'].removeListener('start',listener);
        listener = this.listeners['data'];
        if(listener!==undefined)
            (this.main : Main)['server'].removeListener('data',listener);
        listener = this.listeners['reload'];
        if(listener!==undefined)
            (this.main : Main).removeListener('reload',listener);
    }
}

module.exports = ConsoleChannelModule;