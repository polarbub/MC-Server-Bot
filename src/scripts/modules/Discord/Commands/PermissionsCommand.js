const Module = require('../../../interfaces/Module.js');
const Main = require('../../../interfaces/Main.js');
const DiscordModule = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');

class PermissionsCommand extends Command {

    root : DiscordModule = null;

    constructor(root : DiscordModule) {
        super(root);
        this.root = root;
    }

    register() {
        this.root.commands['permissions'] = this;
        Permissions.addPermission("commands.permissions");
    }

    unregister() {
        delete this.root.commands['permissions'];
    }

    isAllowed(msg: Discord.Message) {
        return Permissions.isUserAllowed(msg,"commands.permissions");
    }

    execute(msg: Discord.Message, args: string) {
        let argList = args.split(" ");
        switch (argList[0]){
            case "list":{
                msg.channel.send(Permissions.list());
                break;
            }
            case "add":{
                if(argList.length < 2)
                    return msg.channel.send("Missing Arguments").catch(console.error);

                let perm = argList[1];
                let users = msg.mentions.members.mapValues(value => value.id).array();
                let roles = msg.mentions.roles.mapValues(value => value.id).array();

                let count = 0;
                users.forEach(id=>{
                    if(Permissions.add(perm,id,'user'))
                        count++;
                })
                roles.forEach(id=>{
                    if(Permissions.add(perm,id,'role'))
                    count++;
                })

                msg.channel.send("Added " + count + " Mentions").catch(console.error);
                break;
            }
            case "remove":{
                if(argList.length < 2)
                    return msg.channel.send("Missing Arguments").catch(console.error);

                let perm = argList[1];
                let users = msg.mentions.members.mapValues(value => value.id).array();
                let roles = msg.mentions.roles.mapValues(value => value.id).array();

                let count = 0;
                users.forEach(id=>{
                    if(Permissions.remove(perm,id,'user'))
                        count++;
                })
                roles.forEach(id=>{
                    if(Permissions.remove(perm,id,'role'))
                    count++;
                })

                msg.channel.send("Removed " + count + " Mentions").catch(console.error);
                break;
            }
            case "save":{
                Permissions.save();
                msg.channel.send("Permissions saved to Disk").catch(console.error);
                break;
            }
            case 'reload': {
                Permissions.reload();
                msg.channel.send("Permissions reloaded from Disk").catch(console.error);
                break;
            }
        }
    }

    getDescription() {
        return "manage the runtime permissions";
    }

    getHelp() {
        let embed = new Discord.MessageEmbed();
        embed.setTitle("Help page for `permissions`");
        embed.setDescription("Sub-Commands:");
        embed.addField("permissions list","show a list of the current configuration");
        embed.addField("permissions save","save the current permissions to Disk");
        embed.addField("permissions reload","reload the permissions from Disk");
        embed.addField("permissions add [permission] (mentions)","add the Mentioned users/roles to the given permission node");
        embed.addField("permissions remove [permission] (mentions)","remove the Mentioned users/roles from the given permission node");
        embed.setFooter("the administrator permission has the special permission node `global`");
        return embed;
    }
}

module.exports = PermissionsCommand;