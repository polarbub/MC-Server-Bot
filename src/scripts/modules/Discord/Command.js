const DiscordBot = require('../DiscordModule.js');
const Discord = require('discord.js');

class Command {

    register(){};

    unregister(){};

    execute(msg, args){};

    getDescription() {} ;

    getHelp() {} ;

    constructor(module){};

    isAllowed(msg) {};
}

module.exports = Command;