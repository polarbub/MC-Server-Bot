const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Discord = require('discord.js');

const ipRegex = /\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/gm;

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

    discordLogger = null;
    buffer: string = "";

    listeners = {};

    onLoad() {
        this.main["ConsoleChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {

            this.getBot().on('ready', this.listeners['ready'] = () => {
                this.getBot().channels.fetch(this.main.getConfigs()['DISCORD_BOT']['CONSOLE_CHANNEL']).then(
                    (channel) => {
                        this.channel = channel;

                        this.discordLogger = setInterval(() => {
                            if (this.channel != null && this.buffer !== "") {
                                this.channel.send(this.buffer.replace(ipRegex, "||CENSORED IP||").replace(/([\\*`'_~])/gm, "\\$&"), {split: true}).catch(console.error);
                                this.buffer = "";
                            }
                        }, 500);

                        this.main['MinecraftServer'].on('start', this.listeners['start'] = (instance) => {
                             instance.stdout.on('data', this.listeners['data'] = (data) => {
                                this.buffer += data;
                            })
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
    }
}

module.exports = ConsoleChannelModule;