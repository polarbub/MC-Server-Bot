const DiscordBot = require('../DiscordModule.js');
const Discord = require('discord.js');

class Command {

    register(){};

    unregister(){};

    execute(msg : Discord.Message, args : string){};

    getDescription(): string {} ;

    getHelp(): string {} ;

    constructor(module: DiscordBot){};
}

module.exports = Command;