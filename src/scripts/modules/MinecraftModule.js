const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const util = require('minecraft-server-util');
const exec = require('child_process').exec;
class MinecraftServer extends Module {

    main : Main;

    constructor(main) {
        super(main);
        this.main = main;
    }

    onLoad() {
        if(this.main['server'] === undefined){
            this.main['server'] = null;
        }

        this.main['MinecraftServer'] = this;
    }

    onUnload() {
        super.onUnload();
    }

    getServer(){
        return this.main['server'];
    }

    start(){
        if(this.main['server'] === null){
            let instance = exec(this.main.getConfigs()['MC_SERVER']['startCMD']);
            instance.stdout.pipe(process.stdout);
            instance.stderr.pipe(process.stderr);
            this.main['server'] = instance;
            instance.on('exit',()=>{
                this.emit('stop',instance);
                this.main['server'] = null;
            })
            this.emit('start',instance);
        }
    }

    stop(){
        if(this.main['server'] != null){
            this.exec('stop');
        }
    }

    exec(cmd, callback = ()=>{}){
        if(this.main['server'] != null){
            let instance = this.main['server'];
            instance.stdin.setEncoding('utf-8');
            let buffer = "";
            let reader = (chunk)=>buffer +=chunk;
            instance.stdout.on('data',reader);
            instance.stdin.write(cmd.trim()+'\r\n');
            setTimeout(()=>{
                instance.stdout.removeListener('data',reader);
                if(buffer!=="")
                    callback(buffer);
            },1000);
        }
    }

    status(callback = ()=>{}, error = ()=>{}){
        util.status(this.main.getConfigs()['MC_SERVER']['ip']).then(callback).catch(error)
    }
}

module.exports = MinecraftServer;