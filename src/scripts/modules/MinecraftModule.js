const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const fs = require("fs");
const path = require('path');
const util = require('minecraft-server-util');
const exec = require('child_process').exec;

class MinecraftServer extends Module {

    main : Main ;

    constructor(main: Main) {
        super(main);
        this.main = main;
    }

    onLoad() {
        if(this.main['server'] === undefined || this.main['MinecraftServer'] === undefined){
            this.main.MinecraftServer = this;
            this.main.server = null;
        }
    }

    onUnload() {
        super.onUnload();
    }

    getServer(){
        return this.main.server;
    }

    start(){
        if(this.main.server === null){
            let instance = exec(this.main.getConfigs().MC_SERVER.startCMD);
            instance.stdout.pipe(process.stdout);
            instance.stderr.pipe(process.stderr);
            this.main.server = instance;
            instance.on('exit',()=>{
                this.main.server = null;
            })
        }
    }

    stop(){
        if(this.main.server != null){
            this.exec('stop');
        }
    }

    exec(cmd, callback = ()=>{}){
        if(this.main.server != null){
            let instance = this.main.server;
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
        }
    }

    status(callback = ()=>{}){
        util.status(this.main.getConfigs().MC_SERVER.ip).then(callback).catch(callback)
    }
}

module.exports = MinecraftServer;