const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Discord = require('discord.js');


class ApplicationReactModule extends Module {

    main: Main = null;

    channel: Discord.TextChannel = null;

    getBot(): Discord.Client {
        return (this.main : Main)['Bot'];
    }

    constructor(main) {
        super(main);
        (this.main : Main) = main;
    }

    listeners = {};

    onLoad() {
        (this.main : Main)["ApplicationChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {
            this.getBot().on('ready', this.listeners['ready'] = () => {
                this.getBot().channels.fetch((this.main : Main).getConfigs()['DISCORD_BOT']['APPLICATION_CHANNEL']).then(
                    (channel) => {
                        this.channel = channel;

                        this.getBot().on('message',this.listeners['message'] = (msg)=>{
                            if(!msg.author.bot)
                                return;
                            if(msg.channel.id !== this.channel?.id)
                                return;
                            if(msg.guild === undefined || msg.guild === null)
                                return;
                            if(msg.channel.permissionsFor(msg.guild.me).has('ADD_REACTIONS', 'VIEW_CHANNEL')){
                                (msg : Message).react("white_check_mark").catch(console.error);
                                (msg : Message).react("x").catch(console.error);
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
        listener = this.listeners['message'];
        if(listener!==undefined)
            this.getBot().removeListener('message',listener);
    }
}

module.exports = ApplicationReactModule;