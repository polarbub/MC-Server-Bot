const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const util = require('minecraft-server-util');
const exec = require('child_process').exec;
class MinecraftServer extends Module {

    main : Main;

    constructor(main) {
        super(main);
        (this.main : Main) = main;
    }

    onLoad() {
        if((this.main : Main)['server'] === undefined){
            (this.main : Main)['server'] = null;
        }

        (this.main : Main)['MinecraftServer'] = this;
    }

    onUnload() {
        super.onUnload();
    }

    getServer(){
        return (this.main : Main)['server'];
    }

    start(){
        if((this.main : Main)['server'] === null){
            let instance = exec((this.main : Main).getConfigs()['MC_SERVER']['startCMD']);
            instance.stdout.pipe(process.stdout);
            instance.stderr.pipe(process.stderr);
            (this.main : Main)['server'] = instance;
            instance.on('exit',()=>{
                (this.main : Main)['server'] = null;
                this.emit('stop',instance);
            })
            this.emit('start',instance);
        }
    }

    stop(){
        if((this.main : Main)['server'] != null){
            this.exec('stop');
        }
    }

    async exec(cmd){
        if((this.main : Main)['server'] != null){
            let instance = (this.main : Main)['server'];
            instance.stdin.setEncoding('utf-8');
            let buffer = "";
            let reader = (chunk)=>buffer +=chunk;
            instance.stdout.on('data',reader);
            await instance.stdin.write(cmd.trim()+'\r\n');
            return new Promise<string>(resolve => {
                setTimeout(()=>{
                    instance.stdout.removeListener('data',reader);
                    resolve(buffer);
                },1000);
            });
        }
        return "";
    }

    status(){
        return util.status((this.main : Main).getConfigs()['MC_SERVER']['ip'])
    }
}

module.exports = MinecraftServer;