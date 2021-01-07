let permissions_file = './configs/permissions.json'
let permissions = require("../../configs/permissions.json");
let Discord = require('discord.js');

function getList(permission, type) {
    let list;
    if (permission === 'global') {
        list = module.exports.config.global["allowed_" + type + "s"]
    } else {
        list = storedPermissions[permission];
        if (list !== undefined)
            list = list["allowed_" + type + "s"];
    }
    return list;
}

let storedPermissions = {};

function getMentions(obj) {
    let mentions = "";
    obj.allowed_users.forEach((id) => {
        mentions += "<@" + id + ">\t";
    });
    mentions += "\r\n\r\n";
    obj.allowed_roles.forEach((id) => {
        mentions += "<@&" + id + ">\t";
    })
    if(mentions.trim().length===0)
        return "{}";
    return mentions.trim();
}

module.exports = {
    config: permissions,
    reload: () => {
        let fs = require('fs');
        let rawdata = fs.readFileSync(permissions_file);
        module.exports.config = JSON.parse(rawdata);
    },
    add: (permission = 'global', id, type = 'user') => {
        let list = getList(permission, type);
        if (list !== undefined && !list.includes(id)) {
            list.push(id);
            return true;
        }
        return false;
    },
    remove: (permission = 'global', id, type = 'user') => {
        let list = getList(permission, type);
        if (list !== undefined && list.includes(id)) {
            let index = list.indexOf(id);
            list.splice(index, 1);
            return true;
        }
        return false;
    },
    save: () => {
        let fs = require('fs');
        fs.writeFileSync(permissions_file, JSON.stringify(module.exports.config, null, 2))
    },
    list: () => {
        let Embed = new Discord.MessageEmbed();
        Embed.setTitle("Permissions:");
        let obj = module.exports.config.global;
        Embed.addField("Bot Administrators", getMentions(obj));

        Object.entries(storedPermissions).forEach(entry =>{
            Embed.addField(entry[0],getMentions(entry[1]));
        })
        return Embed;
    },

    addPermission: (perm) => {
        if (module.exports.config === undefined)
            module.exports.config = {};

        if (module.exports.config.global === undefined)
            module.exports.config.global = {
                allowed_users: [],
                allowed_roles: []
            }

        let steps = perm.split('.');
        let obj = module.exports.config;
        steps.forEach((step) => {
            let new_obj = obj[step];
            if (new_obj === undefined)
                new_obj = obj[step] = {};
            obj = new_obj;
        })

        if (Object.keys(obj).length === 0) {
            obj.allowed_users = [];
            obj.allowed_roles = [];
        }

        storedPermissions[perm] = obj;
    },

    isUserAllowed: function (msg, perm, ignoreAdmin = false) {
        if (!ignoreAdmin)
            if (module.exports.config.global.allowed_users.includes(msg.author.id) || (typeof msg.member !== "undefined" && msg.member !== null
                && msg.member.roles.cache.some((value) => module.exports.config.global.allowed_roles.includes(value.id))))
                return true;

        let obj = storedPermissions[perm];
        if(obj === undefined)
            return false;

        return obj.allowed_users.includes(msg.author.id) || (typeof msg.member !== "undefined" && msg.member !== null
            && msg.member.roles.cache.some((value) => obj.allowed_roles.includes(value.id)));

    },


}