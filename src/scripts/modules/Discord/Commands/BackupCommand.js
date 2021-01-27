const DiscordBot = require('../../DiscordModule.js');
const Command = require('../Command.js');
const Permissions = require('../../../Permissions.js');
const Discord = require('discord.js');
const Colors = require('discord.js').Constants.Colors;

const Backup = require('../../BackupModule.js');


class BackupCommand extends Command {

    root: DiscordBot;

    constructor(module) {
        super(module);
        (this.root: DiscordBot) = (module: DiscordBot);
    }

    register() {
        ((this.root: DiscordBot): DiscordBot).commands['backup'] = this;
        Permissions.addPermission('commands.backup');
    }

    unregister() {
        delete (this.root: DiscordBot).commands['backup'];
    }

    execute(msg: Discord.Message, args) {
        let BackModule: Backup = this.root.main['BackupModule'];
        let Embed = new Discord.MessageEmbed();
        let requester = (msg: Discord.Message).author;
        Embed.setTitle("Backup");
        if (this.root.main['repository'] !== null) {
            args = args.split(' ');
            if (args.length > 0) {
                switch (args[0]) {
                    case 'list': {
                        BackModule.getBackups().then((list) => {
                            Promise.all(list.map(async function (commit) {
                                commit.shorthash = await BackModule.getShortHash(commit.hash);
                                return commit;
                            })).then(list => {
                                let start = 0;

                                let makeEmbed = (start) => {
                                    let Embed = new Discord.MessageEmbed();
                                    Embed.setTitle("Backup");
                                    list.slice(start, start + 5).forEach(commit => {
                                        Embed.addField(commit.shorthash,
                                            "date:\t" + new Date(commit.date).toLocaleString() + "\r\n" +
                                            "comment:\t" + commit.message
                                        );
                                    })
                                    Embed.setDescription((list.length > 0) ? "Listing last available backups\r\nPage " + Number.parseInt((start / 5) + 1) : "No backups found");
                                    Embed.setColor(Colors.BLUE);
                                    return Embed;
                                }

                                Embed = makeEmbed(start);

                                let prepareReact = async (msg) => {
                                    let emoji, collected
                                    emoji = null;
                                    await (msg: Discord.Message).reactions.removeAll().catch(null);
                                    (msg: Discord.Message).react('⬅').catch(console.error);
                                    (msg: Discord.Message).react('➡').catch(console.error);

                                    (msg: Discord.Message).createReactionCollector((reaction, user) => !reaction.me, {idle: 60000, dispose: true}).on('collect',(reaction, user) => {
                                        if(user.id === requester.id) {
                                            emoji = reaction.emoji.name;
                                            switch (emoji) {
                                                case '⬅':
                                                    start = Math.max(start - 5, 0);
                                                    break;
                                                case '➡':
                                                    start = Math.min(start + 5, list.length - 1);
                                                    break;
                                            }
                                            (msg: Discord.Message).edit(makeEmbed(start));
                                        }
                                        reaction.users.remove(user).catch(null);
                                    });
                                }
                                (msg: Discord.Message).channel.send(Embed).catch(console.error).then(prepareReact);
                            })
                        })
                        break;
                    }
                    case 'save': {
                        let comment = args.slice(1).join(' ') || undefined;
                        Embed.setDescription("Backing up");
                        (msg: Discord.Message).channel.send(Embed).catch(console.error);

                        let backupCode = (res) => {
                            Embed.setDescription("Backup Complete");
                            Embed.addField('ID', res.commit);
                            (msg: Discord.Message).channel.send(Embed).catch(console.error);
                        }

                        if (this.root.main['server'] !== null)
                            BackModule.makeServerBackup(comment).then(backupCode).catch(console.error);
                        else
                            BackModule.makeBackup(comment).then(backupCode).catch(console.error);
                        break;
                    }
                    case 'restore': {
                        if (this.root.main['server'] !== undefined) {
                            if(args.length>2){
                                Promise.all(list.map(async function (commit) {
                                    commit.shorthash = await BackModule.getShortHash(commit.hash);
                                    return commit;
                                })).then(async list => {
                                        if(list.some((back)=>back.shorthash === args[2])){
                                            Embed.setTitle("Restore");
                                            Embed.setDescription("Restoring backup: " + args[2]);
                                            Embed.setColor(Colors.GREEN);
                                            (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                            let [branch, reset] = await BackModule.restoreBackup(args[2]).catch(console.error);
                                            Embed.setTitle("Restore");
                                            Embed.setDescription("Backup restored, old data are in  " + branch + " branch");
                                            Embed.setColor(Colors.GREEN);
                                            (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                        }else{
                                            Embed.setTitle("Restore");
                                            Embed.setDescription("Commit not found");
                                            Embed.setColor(Colors.RED);
                                            (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                        }
                                    }
                                );
                            }else
                            BackModule.getBackups().then((list) => {
                                Promise.all(list.map(async function (commit) {
                                    commit.shorthash = await BackModule.getShortHash(commit.hash);
                                    return commit;
                                })).then(list => {
                                    if(list.length === 0)
                                        return;
                                    let curr = 0;

                                    let makeEmbed = (curr) => {
                                        let Embed = new Discord.MessageEmbed();
                                        Embed.setTitle("Restore");
                                        let backup = list[curr];
                                        Embed.setDescription(
                                            "Choose the Backup:\r\n" +
                                            "react with ⬅ or ➡ to select the backup\r\n" +
                                            "react with ✅ to restore the selected backup\r\n" +
                                            "react with ❌ to close the menu"
                                        )
                                        Embed.addField("Id", backup.shorthash, true);
                                        Embed.addField("Date", new Date(backup.date).toLocaleString(), true);
                                        Embed.addField("Comment", backup.message, true);
                                        Embed.setColor(Colors.BLUE);
                                        return Embed;
                                    }

                                    let prepareReact = async (msg) => {
                                        let emoji, collected
                                        emoji = null;
                                        await (msg: Discord.Message).reactions.removeAll().catch(null);
                                        (msg: Discord.Message).react('⬅').catch(console.error);
                                        (msg: Discord.Message).react('✅').catch(console.error);
                                        (msg: Discord.Message).react('❌').catch(console.error);
                                        (msg: Discord.Message).react('➡').catch(console.error);

                                        (msg: Discord.Message).createReactionCollector((reaction, user) => !reaction.me, {
                                            idle: 60000,
                                            dispose: true
                                        }).on('collect', async(r, u) => {
                                            if (r.users.cache.has(requester.id)) {
                                                emoji = r.emoji.name;
                                                switch (emoji) {
                                                    case '⬅':
                                                        curr = Math.max(curr - 1, 0);
                                                        break;
                                                    case '➡':
                                                        curr = Math.min(curr + 1, list.length - 1);
                                                        break;
                                                    case '❌':
                                                        msg.delete();
                                                        return;
                                                    case '✅':
                                                        Embed.setTitle("Restore");
                                                        Embed.setDescription("Restoring backup: " + list[curr].shorthash);
                                                        Embed.setColor(Colors.GREEN);
                                                        (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                                        let [branch, reset] = await BackModule.restoreBackup(list[curr].hash).catch(console.error);
                                                        Embed.setTitle("Restore");
                                                        Embed.setDescription("Backup restored, old data are in  " + branch + " branch");
                                                        Embed.setColor(Colors.GREEN);
                                                        (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                                        break;
                                                    default:
                                                        return;
                                                }
                                                (msg: Discord.Message).edit(makeEmbed(curr));
                                            }
                                            r.users.remove(u).catch(null);
                                        });
                                    }
                                    (msg: Discord.Message).channel.send(makeEmbed(curr)).catch(console.error).then(prepareReact);
                                })
                            })
                        } else {
                            Embed.setTitle("Restore");
                            Embed.setDescription("Please stop the server first");
                            Embed.setColor(Colors.RED);
                            (msg: Discord.Message).channel.send(Embed).catch(console.error)
                        }
                        break;
                    }
                }
            } else {
                Embed.setDescription("No command Specified");
                Embed.setColor(Colors.RED);
                (msg: Discord.Message).channel.send(Embed).catch(console.error);
            }
        } else {
            Embed.setDescription("Server is not Running");
            Embed.setColor(Colors.BLUE);
            (msg: Discord.Message).channel.send(Embed).catch(console.error);
        }
    }

    getDescription() {
        return "manages backups on the server";
    }

    getHelp(): Discord.MessageEmbed {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Help for `backup`");
        Embed.setDescription(this.getDescription());
        Embed.addField("list","shows a interactive list of the available backups");
        Embed.addField("save (comment)","makes a immediate backup\r\nif comment is specified add it to the commit");
        Embed.addField("restore (Backup ID)","Rollback command, restores the specified ID\r\n if no ID is specified an interactive menu shows up");
        return Embed;
    }

    isAllowed(msg: Discord.Message) {
        return Permissions.isUserAllowed((msg: Discord.Message), 'commands.backup');
    }
}

module.exports = BackupCommand;
