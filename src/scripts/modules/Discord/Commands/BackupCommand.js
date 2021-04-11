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
        if(BackModule.runningGit){
            Embed.setDescription("Git Already Running please wait");
            Embed.setColor("RED");
            (msg: Discord.Message).channel.send(Embed).catch(console.error);
            return;
        }
        if (this.root.main['repository'] !== null) {
            args = args.split(' ');
            if (args.length > 0) {
                switch (args[0]) {
                    case 'list': {
                        let Embed = new Discord.MessageEmbed();
                        Embed.setTitle("Backup");
                        Embed.setDescription('Fetching data');


                        let makeEmbed = (list,end=false) => {
                            let Embed = new Discord.MessageEmbed();
                            Embed.setTitle("Backup");
                            list.forEach(commit => {
                                let date = new Date(commit.date);
                                Embed.addField(commit.shorthash,
                                    "date:\t" + date.toLocaleString() + "\r\n" +
                                    "comment:\t" + commit.message
                                );
                            })
                            Embed.setDescription((list.length > 0) ? "Listing last available backups\r\nPage: " + Math.ceil((cursor+1)/5) + "/" + Math.ceil((count+1)/5): "No backups found");
                            Embed.setColor(Colors.BLUE);
                            Embed.setFooter((!end)?`Listening to ${msg.author.tag} Reactions only`:"Not Listening to Reactions Anymore")
                            return Embed;
                        }

                        let displayList: Array;
                        let cursor = 0;

                        let count = 0;

                        let makeList = async (new_cursor=0) => {
                            count = Number.parseInt(await BackModule.getRepository().raw(['rev-list', '--count', 'HEAD']).catch(console.error));

                            let opt = {
                                '--max-count': 5,
                                '--skip':new_cursor,
                                'HEAD': null
                            }

                            let result = await BackModule.getRepository().log(opt);
                            return [(await Promise.all(result?.all.map(async function (commit) {
                                commit.shorthash = await BackModule.getShortHash(commit.hash);
                                return commit;
                            }))).sort((a, b) => {
                                return -a.date.localeCompare(b.date)
                            }),new_cursor];
                        }

                        let prepareReact = async (msg) => {
                            let emoji
                            emoji = null;
                            await (msg: Discord.Message).reactions.removeAll().catch(null);
                            (msg: Discord.Message).react('⬅').catch(console.error);
                            (msg: Discord.Message).react('➡').catch(console.error);

                            (msg: Discord.Message).createReactionCollector((reaction, user) => !reaction.me, {
                                idle: 60000,
                                dispose: true
                            }).on('collect', async (reaction, user) => {
                                let list;
                                let new_cur;
                                if (user.id === requester.id) {
                                    emoji = reaction.emoji.name;
                                    switch (emoji) {
                                        case '⬅':
                                            [list,new_cur] =  (await makeList(cursor-5) : Array);
                                            break;
                                        case '➡':
                                            [list,new_cur]= (await makeList(cursor+5) : Array);
                                            break;
                                    }
                                    if(list.length > 0) {
                                        displayList = list;
                                        cursor = new_cur;
                                        (msg: Discord.Message).edit(makeEmbed(displayList)).catch(console.error);
                                    }
                                }
                                reaction.users.remove(user).catch(null);
                            }).on('end',()=>{
                                (msg: Discord.Message).edit(makeEmbed(displayList,true)).catch(console.error);
                            });
                        }


                        (msg: Discord.Message).channel.send(Embed).catch(console.error).then(msg=>{
                            makeList().then(([_list: Array,update_cursor])=>{
                                if(_list.length > 0) {
                                    displayList= _list;
                                    cursor = update_cursor;
                                    (msg: Discord.Message).edit(makeEmbed(_list)).catch(console.error).then(prepareReact);
                                }else{
                                    let Embed = new Discord.MessageEmbed();
                                    Embed.setTitle("Backup");
                                    Embed.setDescription("No backup found");
                                    Embed.setColor("RED");
                                    (msg: Discord.Message).edit(Embed).catch(console.error)
                                }
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
                        if (this.root.main['server'] === null) {
                            let commit = "HEAD"
                            let cursor = 0;
                            let count = 0;


                            let makeEmbed = (curr,end=false) => {
                                let Embed = new Discord.MessageEmbed();
                                Embed.setTitle("Restore");
                                Embed.setDescription(
                                    "Choose the Backup:\r\n" +
                                    `Commit: ${cursor+1}/${count}\r\n\r\n`+
                                    "react with ⬅ or ➡ to select the backup\r\n" +
                                    "react with ✅ to restore the selected backup\r\n" +
                                    "react with ❌ to close the menu"
                                )
                                Embed.addField("\u200B","\u200B");
                                Embed.addField("Id", curr.shorthash, true);
                                Embed.addField("Date", new Date(curr.date).toLocaleString(), true);
                                Embed.addField("Comment", curr.message, true);
                                Embed.setColor(Colors.BLUE);
                                Embed.setFooter((!end)?`Listening to ${msg.author.tag} Reactions only`:"Not Listening to Reactions Anymore")

                                return Embed;
                            }

                            let fetchCommit = async (cursor)=>{
                                let new_count = Number.parseInt(await BackModule.getRepository().raw(['rev-list', '--count', 'HEAD']).catch(console.error));

                                if(count!==0)
                                    cursor += new_count-count;
                                count = new_count;

                                let opt = {
                                    '--max-count': 1,
                                    '--skip': cursor,
                                    'HEAD': null
                                }

                                let result = await BackModule.getRepository().log(opt);
                                let list = await Promise.all(result?.all.map(async function (commit) {
                                    commit.shorthash = await BackModule.getShortHash(commit.hash);
                                    return commit;
                                }));
                                return [list?.[0],cursor];
                            }

                            let curr;

                            let prepareReact = async (msg) => {
                                let emoji
                                emoji = null;
                                await (msg: Discord.Message).reactions.removeAll().catch(null);
                                (msg: Discord.Message).react('⏪').catch(console.error);
                                (msg: Discord.Message).react('⬅').catch(console.error);
                                (msg: Discord.Message).react('✅').catch(console.error);
                                (msg: Discord.Message).react('❌').catch(console.error);
                                (msg: Discord.Message).react('➡').catch(console.error);
                                (msg: Discord.Message).react('⏩').catch(console.error);

                                (msg: Discord.Message).createReactionCollector((reaction, user) => !reaction.me, {
                                    idle: 60000,
                                    dispose: true
                                }).on('collect', async (r, u) => {
                                    if (r.users.cache.has(requester.id)) {
                                        emoji = r.emoji.name;
                                        let commit,curs
                                        switch (emoji) {
                                            case '⬅':
                                                [commit,curs] = await fetchCommit(cursor-1);
                                                if(commit===undefined)
                                                    cursor = Math.max(curs+1,0);
                                                else {
                                                    curr=commit;
                                                    cursor = Math.max(curs,0);
                                                }
                                                break;
                                            case '⏪':
                                                [commit,curs] = await fetchCommit(cursor-10);
                                                if(commit===undefined)
                                                    cursor = Math.max(curs+10,0);
                                                else {
                                                    curr=commit;
                                                    cursor = Math.max(curs,0);
                                                }
                                                break;
                                            case '➡':
                                                [commit,curs] = await fetchCommit(cursor+1);
                                                if(commit===undefined)
                                                    cursor = Math.min(curs-1,count);
                                                else{
                                                    curr=commit;
                                                    cursor =  Math.min(curs,count);
                                                }
                                                break;
                                            case '⏩':
                                                [commit,curs] = await fetchCommit(cursor+10);
                                                if(commit===undefined)
                                                    cursor = Math.min(curs-10,count);
                                                else{
                                                    curr=commit;
                                                    cursor =  Math.min(curs,count);
                                                }
                                                break;
                                            case '❌':
                                                msg.delete();
                                                return;
                                            case '✅':
                                                msg.delete();
                                                Embed.setTitle("Restore");
                                                Embed.setDescription("Restoring backup: " + curr.shorthash);
                                                Embed.setColor(Colors.GREEN);
                                                (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                                let [branch, reset] = await BackModule.restoreBackup(curr.hash).catch(console.error);
                                                Embed.setTitle("Restore");
                                                Embed.setDescription("Backup restored, old data are in  " + branch + " branch");
                                                Embed.setColor(Colors.GREEN);
                                                (msg: Discord.Message).channel.send(Embed).catch(console.error)
                                                break;
                                            default:
                                                return;
                                        }

                                        (msg: Discord.Message).edit(makeEmbed(curr)).catch(console.error);
                                    }
                                    r.users.remove(u).catch(null);
                                }).on('end',()=>{
                                    (msg: Discord.Message).edit(makeEmbed(curr,true)).catch(console.error);
                                });

                            }


                            let Embed = new Discord.MessageEmbed();
                            Embed.setTitle("Backup");
                            Embed.setDescription('Fetching data');

                            (msg: Discord.Message).channel.send(Embed).catch(console.error).then(async (msg)=>{
                                if (args.length > 1) {
                                    let value = await BackModule.getRepository().raw(['rev-list','--count','HEAD...'+args[1]]).catch(()=>{});
                                    value = Number.parseInt(value) || 0;
                                    cursor = value;
                                }
                                let curs;
                                [curr,curs] = await fetchCommit(cursor);
                                if(curr===undefined){
                                    Embed.setDescription("No Backup found");
                                    Embed.setColor("RED");
                                    msg.edit(Embed).catch(console.error);
                                }else{
                                    msg.edit(makeEmbed(curr)).then(prepareReact).catch(console.error);
                                }
                            });
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
        Embed.addField("list", "shows a interactive list of the available backups");
        Embed.addField("save (comment)", "makes a immediate backup\r\nif comment is specified add it to the commit");
        Embed.addField("restore (Backup ID)", "Rollback command, restores the specified ID\r\n if no ID is specified an interactive menu shows up");
        return Embed;
    }

    isAllowed(msg: Discord.Message) {
        return Permissions.isUserAllowed((msg: Discord.Message), 'commands.backup');
    }
}

module.exports = BackupCommand;
