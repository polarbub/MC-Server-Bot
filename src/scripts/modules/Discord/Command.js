const DiscordBot = require('../DiscordModule.js');
const Discord = require('discord.js');

class Command {

    register(){};

    unregister(){};

    execute(msg : Discord.Message, args){};

    getDescription() : string {} ;

    getHelp() : Discord.MessageEmbed{} ;

    constructor(module : DiscordBot){};

    isAllowed(msg : Discord.Message) {};
}

module.exports = Command;