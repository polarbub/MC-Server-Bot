let permissions = require('../../../configs/permissions.json');

module.exports = {
    config: permissions,
    reload: () => {
        let fs = require('fs');
        let rawdata = fs.readFileSync('./lib/configs/permissions.json');
        module.exports.config = JSON.parse(rawdata);
    },
    add: (command = 'global', id, type = 'user') => {
        let list;
        if (command === 'global') {
            list = module.exports.config.global["allowed_" + type + "s"]
        } else {
            list = module.exports.config.commands[command];
            if (list !== undefined)
                list = list["allowed_" + type + "s"];
        }
        if (list !== undefined && !list.includes(id)) {
            list.push(id);
        }
    },
    remove: (command = 'global', id, type = 'user') => {
        let list;
        if (command === 'global') {
            list = module.exports.config.global["allowed_" + type + "s"]
        } else {
            list = module.exports.config.commands[command];
            if (list !== undefined)
                list = list["allowed_" + type + "s"];
        }
        if (list !== undefined && list.includes(id)) {
            let index = list.indexOf(id);
            list.splice(index, 1);
        }
    },
    save: () => {
        let fs = require('fs');
        fs.writeFileSync('./lib/configs/permissions.json', JSON.stringify(module.exports.config, null, 2))
    },
    list: () => {
        return JSON.stringify(module.exports.config, null, 4)
    },
    fix_missing: (Discord) => {
        if (module.exports.config === undefined)
            module.exports.config = {};

        if (module.exports.config.global === undefined)
            resmodule.exports.configtrictions.global = {
                allowed_users: [],
                allowed_roles: []
            }

        if (module.exports.config.commands === undefined) {
            module.exports.config.commands = {}
        }

        let list = Object.entries(Discord.commands);

        list.forEach((entry) => {
            if (module.exports.config.commands[entry[0]] === undefined) {
                module.exports.config.commands[entry[0]] = {
                    allowed_users: [],
                    allowed_roles: []
                };
            }
        })
    },
    isUserAllowed: function (msg, command) {
        return module.exports.config.global.allowed_users.includes(msg.author.id) || module.exports.config.commands[command].allowed_users.includes(msg.author.id) ||
            (typeof msg.member !== "undefined" && msg.member !== null
                && msg.member.roles.cache.some((value, key) => module.exports.config.global.allowed_roles.includes(value.id) || module.exports.config.commands[command].allowed_roles.includes(value.id)));
    },


}