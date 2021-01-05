const Main = require('./interfaces/Main.js');
const Module = require('./interfaces/Module.js');
const Permissions = require('./Permissions.js');
const config = require('../../configs/config.json');


const fs = require("fs");
const path = require('path');


//log errors to file
try {
    fs.mkdirSync('./logs/');
}catch (e){}
const errorWriter = fs.createWriteStream('./logs/error.log',{flags:'a'});
process.stderr.pipe(errorWriter);

class Program extends Main {

    modules = {};

    getPermissions() {
        return Permissions.config;
    }

    getConfigs() {
        return config;
    }

    onStart() {
        this.instance = this;
        fs.readdir('./bin/scripts/modules',(err,files) => {
            if (err) {
                return console.log('Unable to scan modules directory: ' + err);
            }

            files.forEach((file) => {
                if(path.extname(file) === ".js") {
                    file = path.resolve("./bin/scripts/modules") + "/" + file;
                    let mod = require(file);
                    let instance = new mod(this);
                    instance.onLoad();
                    this.modules[file] = instance;
                }
            });
        })
    }

    onStop() {
        this.modules.forEach(function (mod){
            mod.onUnload();
        })
    }

}

let main = new Program();
main.onStart();

return;

const Bot = require('./DiscordBot.js');
const Mc = require('./McServer.js');

let Discord = Bot.init();
let Server = undefined;

let channel = null;
let buffer = "";

let periodicCheck;

function start_server(msg) {
    if (typeof Server === "undefined") {
        Server = Mc.start(config);

        if (Server.exitCode === null) {
            msg.channel.send("`Starting Server`").catch(console.error);
            Server.stdout.on('data', (chunk) => {
                buffer += chunk;
            })

            Discord.user.setActivity("Server Startup", {type: "WATCHING"});

            let doneHandler = (chunk) => {
                if (chunk.includes("Done")) {
                    msg.channel.send("`Server Started`").catch(console.error);
                    Server.stdout.removeListener('data', doneHandler);
                    Discord.user.setActivity("Players on the Server", {type: "WATCHING"});

                    periodicCheck = setInterval(() => {
                        Mc.status(config, Server, (res) => {
                            Discord.user.setActivity(res.onlinePlayers + " Players on the Server", {type: "WATCHING"});
                        })
                    }, 30000)
                }
            }
            Server.stdout.on('data', doneHandler);
            Server.on('exit', () => {
                Server = undefined;
                if (periodicCheck !== undefined) {
                    clearInterval(periodicCheck);
                    periodicCheck = undefined;
                }
                Discord.user.setActivity("To Commands", {type: "LISTENING"});
            })
        } else {
            msg.channel.send("`Something went wrong`").catch(console.error);
        }
    } else {
        msg.channel.send("`Server already running`").catch(console.error);
    }
}

Bot.addCommand(config, Discord, 'start', async (msg, args) => {
    start_server(msg);
}, description = "Start the Minecraft Server");


Bot.addCommand(config, Discord, 'stop', async (msg, args) => {
    if (typeof Server !== "undefined") {
        Mc.stop(config, Server);
        msg.channel.send("`Stopping server`").catch(console.error);

        Discord.user.setActivity("Server SHUTDOWN", {type: "WATCHING"});

        Server.on('exit', () => {
            msg.channel.send("`Server stopped`").catch(console.error);
        })
    } else {
        msg.channel.send("`Server not running`").catch(console.error);
    }
}, description = "Stop the Minecraft Server");


Bot.addCommand(config, Discord, 'restart', async (msg, args) => {
    if (typeof Server !== "undefined") {
        Mc.stop(config, Server);
        msg.channel.send("`Stopping server'").catch(console.error);
        Server.on('exit', () => {
            msg.channel.send("`Server stopped`").catch(console.error);
            setImmediate(() => {
                start_server(msg);
            });
        })

    } else {
        msg.channel.send("`Server not running`").catch(console.error);
    }
}, description = "Start the Minecraft Server");

Bot.addCommand(config, Discord, 'exec', async (msg, args) => {
    if (typeof Server !== "undefined") {
        Mc.exec(config, Server, args, (reponse) => {
            if (msg.channel !== channel)
                msg.channel.send("```" + reponse + "```", {split: true}).catch(console.error);
        });
    } else {
        msg.channel.send("`Server not running`").catch(console.error);
    }
}, description = "Execute a command on the Minecraft Server");


Bot.addCommand(config, Discord, 'status', async (msg, args) => {
    if (typeof Server !== "undefined") {
        Mc.status(config, Server, (reponse) => {
            msg.channel.send("Server is running " + reponse.version +
                "\r\nwith " + reponse.onlinePlayers + " of " + reponse.maxPlayers
                , {split: true}).catch(console.error);
        });
    } else {
        msg.channel.send("`Server not running`").catch(console.error);
    }
}, description = "get the status of the Minecraft Server");


Bot.addCommand(config, Discord, 'restrictions', async (msg, args) => {
    let argsList = args.split(' ');
    switch (argsList[0]) {
        case 'add': {
            let users = msg.mentions.members.mapValues(value => value.id).array();
            let roles = msg.mentions.roles.mapValues(value => value.id).array();
            setImmediate(() => {
                users.forEach(user => {
                    restrictions.add(command = argsList[1], id = user, type = 'user');

                })
                setImmediate(() => {
                    roles.forEach(role => {
                        restrictions.add(command = argsList[1], id = role, type = 'role');

                    })
                })
            })
            msg.channel.send("Mentions added").catch(console.error);
            break;
        }
        case 'remove': {
            let users = msg.mentions.members.mapValues(value => value.id).array();
            let roles = msg.mentions.roles.mapValues(value => value.id).array();
            setImmediate(() => {
                users.forEach(user => {
                    restrictions.remove(command = argsList[1], id = user, type = 'user');
                })
                setImmediate(() => {
                    roles.forEach(role => {
                        restrictions.remove(command = argsList[1], id = role, type = 'role');

                    })
                })
            })
            msg.channel.send("Mentions removed").catch(console.error);
            break;
        }
        case 'save': {
            restrictions.save();
            msg.channel.send("Restrictions saved").catch(console.error);
            break;
        }
        case 'reload': {
            restrictions.reload();
            msg.channel.send("Restrictions reloaded").catch(console.error);
            break;
        }
        case 'list': {
            let ret = restrictions.list();
            msg.channel.send("Restrictions: " + "\r\n```json\r\n" + ret + "\r\n```", {split: true}).catch(console.error);
            break;
        }

    }
}, description = "changes the restrictions at runtime");

Bot.addCommand(config, Discord, 'help', async (msg, args) => {
    let string = "";
    string += "Commands:\r\n```";
    Object.entries(Discord.commands).filter((entry) => Bot.isUserAllowed(msg, entry[0])
    ).forEach((entry) => {
        string += entry[0] + "\t" + entry[1].description + "\r\n";
    })
    msg.channel.send(string + "```", {split: true}).catch(console.error);
}, description = "Shows this help page");

Bot.start(config, Discord);

let ipRegex = /\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/gm;

Discord.on('ready', () => {
    Discord.channels.fetch(config.DISCORD_BOT.CONSOLE_CHANNEL)
        .then((loc_channel) => channel = loc_channel).catch(console.error);

    Discord.user.setActivity("To Commands", {type: "LISTENING"});

    setInterval(() => {
        if (Server !== undefined && channel != null)
            if (buffer !== "")
                channel.send(buffer.replace(ipRegex, "||CENSORED IP||").replace(/([\\\*\`\'\_\~\`])/gm, "\\$&"), {split: true}).catch(console.error);
        buffer = "";
    }, 1000)
})

restrictions.fix_missing(Discord);
restrictions.save();
