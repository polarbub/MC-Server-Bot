const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Command = require('./Discord/Command.js');
const Permissions = require('../Permissions.js');
const Discord = require('discord.js');
const fs = require("fs");
const path = require('path');

const ipRegex = /\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/gm;

class ConsoleChannelModule extends Module{


    main : Main = null;

    channel : Discord.TextChannel = null;

    getBot() : Discord.Client {
        return this.main['Bot'];
    }

    constructor(main) {
        super(main);
        this.main = main;
    }

    discordLogger = null;
    buffer : string = "";

    onLoad() {
        this.main["ConsoleChannel"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(()=>{
            this.getBot().channels.fetch(this.main.getConfigs().DISCORD_BOT.CONSOLE_CHANNEL).then(
                (channel)=>{
                    this.channel = channel;

                this.discordLogger = setInterval(()=>{
                    if(this.channel!= null && this.buffer !== ""){
                        this.channel.send(this.buffer.replace(ipRegex, "||CENSORED IP||").replace(/([\\\*\`\'\_\~\`])/gm, "\\$&"), {split: true}).catch(console.error);
                        this.buffer = "";
                    }
                },500);

                this.main['MinecraftServer'].on('start',(instance)=>{
                    instance.stdout.on('data',(data)=>{
                        this.buffer += data;
                    })
                })

                }
            ).catch(console.error);

        });
    }

    onUnload() {
        super.onUnload();
    }
}

module.exports=ConsoleChannelModule;