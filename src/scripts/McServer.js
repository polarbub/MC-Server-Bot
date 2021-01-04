
const util = require('minecraft-server-util');
const exec = require('child_process').exec;
const stream = require("stream");

module.exports = {
    start: (config) => {
        let instance = exec(config.MC_SERVER.startCMD, (error, stdout, stderr) => {
            console.log('Output -> ' + stdout);
            if (error !== null) {
                console.log("Error -> " + error);
            }
        });
        instance.stdout.pipe(process.stdout);
        return instance;
    },

    stop: (config, instance) => {
        module.exports.exec(config,instance,'stop');
    },

    exec: (config, instance, command, callback=()=>{}) => {
            instance.stdin.setEncoding('utf-8');
            let buffer = "";
            let reader = (chunk)=>buffer +=chunk;
            instance.stdout.on('data',reader);
            instance.stdin.write(command.trim()+'\r\n');
            setTimeout(()=>{
                instance.stdout.removeListener('data',reader);
                if(buffer!=="")
                    callback(buffer);
            },1000);
    },

    status: (config, instance, callback)=>{
        util.status(config.MC_SERVER.ip).then(callback).catch(callback)
    }
}