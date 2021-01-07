const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const MinecraftServer = require('../modules/MinecraftModule.js');
const simpleGit = require('simple-git');
const path = require('path');


class BackupModule extends Module {

    main: Main = null;

    constructor(main : Main) {
        super(main);
        this.main = main;
    }

    listeners = {};

    mcInstance = null;

    onLoad() {
        if(this.main['repository'] === undefined){
            let folder = path.resolve(this.main.getConfigs()['MC_SERVER']['server_path']);
            let repo = simpleGit(folder);
            this.main['repository'] = repo;
            repo.cwd(folder);
            repo.checkIsRepo('root').then((res)=>{
                if(!res){
                    repo.init().then(()=>this.makeBackup()).catch(console.error);
                }
            }).catch(console.error);

            setImmediate(()=>{
                this.mcInstance = this.main['MinecraftServer'];
                this.main['MinecraftServer'].on('start',this.listeners['start'] = ()=>{
                    this.listeners['interval'] = setInterval( ()=>{
                        this.main['MinecraftServer'].exec('say Backup in 30 seconds if flying please land');
                        setTimeout(()=>{
                            this.main['MinecraftServer'].exec('say Backing up');
                            this.main['MinecraftServer'].exec('save-off');
                            this.main['MinecraftServer'].exec('save-all');
                            this.makeBackup().then((res)=>{
                                this.main['MinecraftServer'].exec('save-on');
                                this.main['MinecraftServer'].exec('say Backup complete, id:' + res.hash);
                            }).catch(console.error)
                        },30);
                    },3600);
                })

                this.main['MinecraftServer'].on('stop',this.listeners['stop'] = ()=>{
                    clearInterval(this.listeners['interval']);
                    this.makeBackup().catch(console.error);
                })

                this.main.on('reload',this.listeners['reload'] = (old_module,new_module) =>{
                    if(old_module === this.mcInstance)
                        this.mcInstance = new_module;

                    new_module.on('start',this.listeners['start']);
                    new_module.on('stop',this.listeners['stop']);
                })
            });
        }

        this.main['BackupModule'] = this;
    }

    async makeBackup(msg=new Date().toLocaleString()){
        let repo = this.getRepository();
        await repo.add('.').catch(console.error);
        return await repo.commit(msg, ['.'], ['--author="Backup <Backup@localhost>"', '--allow-empty']).catch(console.error);
    }

    async getBackups(count=10){
        let repo = this.getRepository();
        let res = await repo.log().catch(console.error)
        let list = res.all;
        list.sort((a,b)=>{ return -a.date.localeCompare(b.date)});
        list = list.slice(0,count);
        return list;
    }

    async restoreBackup(backup='latest'){
        let repo = this.getRepository();
        let res = await repo.log().catch(console.error)
        let backObj = null;
        if(backup==="latest"){
            backObj = res.latest;
        }else{
            backObj = res.all.find((obj)=>{
                if(obj.string === backup)
                    return true;
                if(obj.date === backup)
                    return true;
                if(obj.message === backup)
                    return true;
                return false;
            });
        }

        if(backObj!==null){
            await this.makeBackup("pre rollback");

            let branch = await repo.branch(['-C','rollback ' + new Date.toLocaleString()]);
            console.log(branch);
            let reset = await repo.reset('hard',[backObj.hash]);
            return [branch , reset];
        }
    }

    getShortHash(hash){
        let repo = this.getRepository();
        return repo.revparse(['--short', hash]);
    }

    onUnload() {
        this.main['MinecraftServer'].removeListener('start',this.listeners['start']);
        this.main['MinecraftServer'].removeListener('stop',this.listeners['stop']);
        clearInterval(this.listeners['interval']);
        this.main.removelistener('reload',this.listeners['reload']);
    }

    getRepository(){
        return this.main['repository'];
    }

}

module.exports = BackupModule;