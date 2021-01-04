const Discord = require('discord.js');
let restrictions = require('./Restrictions.js');

module.exports = {
    init: () => {
        let instance = new Discord.Client();
        return instance;
    },

    start: (config, instance) => {
         instance.login(config.DISCORD_BOT.TOKEN).catch(console.error);
         return instance;
    },

    isUserAllowed: function (msg, command) {
        return restrictions.config.global.allowed_users.includes(msg.author.id) || restrictions.config.commands[command].allowed_users.includes(msg.author.id) ||
            (typeof msg.member !== "undefined" && msg.member !== null
                && msg.member.roles.cache.some((value, key) => restrictions.config.global.allowed_roles.includes(value.id) || restrictions.config.commands[command].allowed_roles.includes(value.id)));
    },

    addCommand: (config, instance, command, callback, description="", help="") =>{
        let handler = instance.on('message', msg => {
                if(msg.cleanContent.startsWith(config.DISCORD_BOT.PREFIX)){
                    if(msg.cleanContent.substr(1).split(' ')[0].startsWith(command)){
                        if(module.exports.isUserAllowed(msg, command))
                                                callback(msg,msg.cleanContent.substr(command.length+2));
                    }
                }
        });
        if ( typeof instance.commands === "undefined"){
            instance.commands = {};
        }
        instance.commands[command] = {handler:handler,description:description, help:help};
    }
}