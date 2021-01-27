const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const Backup = require('../../BackupModule.js');


class ExecCommand extends Command {

    root : DiscordBot;

    constructor(module) {
        super(module);
        (this.root : DiscordBot) = ( module : DiscordBot );
    }

    register() {
        ((this.root : DiscordBot) : DiscordBot).commands['backup'] = this;
        Permissions.addPermission('commands.backup');
    }

    unregister() {
        delete (this.root : DiscordBot).commands['backup'];
    }

    execute(msg : Discord.Message, args) {
        let BackModule : Backup = this.root.main['BackupModule'];
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Backup");
        if(this.root.main['repository'] !== null) {
            args = args.split(' ');
            if (args.length > 0) {
                switch (args[0]){
                    case 'list':{
                        let count = Number.parseInt(args[1]) || 10;
                        BackModule.getBackups(count).then((list)=>{
                            Promise.all( list.map(async function(commit){
                                commit.hash = await BackModule.getShortHash(commit.hash);
                                return commit;
                            })).then(list=> {
                                list.forEach(commit => {
                                    Embed.addField(commit.hash,
                                        "date:\t" + new Date(commit.date).toLocaleString() + "\r\n" +
                                        "comment:\t" + commit.message
                                    );
                                })
                                Embed.setDescription((list.length > 0) ? "Listing last "+count+" available backups" : "No backups found");
                                Embed.setColor(Colors.BLUE);
                                (msg: Discord.Message).channel.send(Embed).catch(console.error);
                            })
                        })
                        break;
                    }
                    case 'save':{
                        let comment = args.slice(1).join(' ') || undefined;
                        Embed.setDescription("Backing up");
                        (msg: Discord.Message).channel.send(Embed).catch(console.error);

                        let backupCode = (res)=>{
                            Embed.setDescription("Backup Complete");
                            Embed.addField(res.commit,"date:\t" + new Date(res.date).toLocaleString() +"\r\n" +
                                "comment:\t" + res.message);
                            (msg: Discord.Message).channel.send(Embed).catch(console.error);
                        }

                        if(this.root.main['server']!==null)
                            BackModule.makeServerBackup(comment).then(backupCode).catch(console.error);
                        else
                            BackModule.makeBackup(comment).then(backupCode).catch(console.error);
                    }
                }
            } else {
                Embed.setDescription("No command Specified");
                Embed.setColor(Colors.RED);
                (msg : Discord.Message).channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor(Colors.BLUE);
            (msg : Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "executes commands on the game console";
    }

    getHelp(): Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `exec`");
        Embed.setDescription(this.getDescription());
        return Embed;
    }

    isAllowed(msg : Discord.Message) {
        return Permissions.isUserAllowed((msg : Discord.Message), 'commands.exec');
    }
}

module.exports = ExecCommand;
