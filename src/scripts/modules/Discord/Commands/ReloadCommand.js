const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const DiscordBot = require('../../DiscordModule.js');

class ReloadCommand extends Command {

    root;

    constructor(module) {
        super(module);
        (this.root : DiscordBot) = module;
    }

    register() {
        (this.root : DiscordBot).commands['reload'] = this;
        Permissions.addPermission('commands.reload');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['reload'];
    }

    execute(msg, args) {
        let argList = args.split(/ +/gm);
        switch (argList[0]){
            case "commands":{
                let Embed = new Discord.MessageEmbed();
                Embed.setTitle("Bot Reloading")
                Embed.setDescription("Started Reloading commands");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);

                (this.root : DiscordBot).reloadCommands();

                Embed = new Discord.MessageEmbed();
                Embed.setTitle("Bot Reloading")
                Embed.setDescription("Finished Reloading commands");
                Embed.setFooter("things might behave weird, in that case stop and restart the program");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
                break;
            }
            case "config":{
                let Embed = new Discord.MessageEmbed();
                Embed.setTitle("Bot Reloading")
                Embed.setDescription("Started Reloading Config");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);

                (this.root : DiscordBot).main.LoadConfig();

                Embed = new Discord.MessageEmbed();
                Embed.setTitle("Bot Reloading")
                Embed.setDescription("Finished Reloading configs");
                Embed.setFooter("this wont affect anything till you reload the affected module");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
                break;
            }
            case "module":{
                let module_name = argList[1];

                if(module_name === null || module_name === undefined || module_name === ""){
                    let Embed = new Discord.MessageEmbed();
                    Embed.setTitle("Program HotLoading")
                    Embed.setDescription("Missing module name");
                    Embed.setColor("#ff0000");
                    (msg : Discord.Message).channel.send(Embed).catch(console.error);
                }
                let Embed = new Discord.MessageEmbed();
                Embed.setTitle("Program HotLoading")
                Embed.setDescription("Started HotReload of module");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);

                (this.root : DiscordBot).main.reloadModule(module_name);

                Embed = new Discord.MessageEmbed();
                Embed.setTitle("Program HotLoading")
                Embed.setDescription("finished HotReload of module");
                Embed.setFooter("things might behave weird, in that case stop and restart the program");
                Embed.setColor("#ff0000");
                (msg : Discord.Message).channel.send(Embed).catch(console.error);

                break;
            }
        }
    }

    getDescription() {
        return "reload the Bot scripts";
    }

    getHelp() : Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `reload`");
        Embed.setDescription("Sub-Commands:");
        Embed.addField("reload commands","reloads all the bot commands");
        Embed.addField("reload config","reloads config file from disk");
        Embed.addField("reload module [FileName]","reloads the specified FileName from memory\r\nUnloads it if it got deleted\r\nLoads it if got added");
        Embed.setFooter("this is a really dangerous command use it only if you know what you're doing");
        return Embed;
    }

    isAllowed(msg) {
        return Permissions.isUserAllowed(msg, 'commands.reload');
    }
}

module.exports = ReloadCommand;
