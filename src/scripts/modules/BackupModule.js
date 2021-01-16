const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const MinecraftServer = require('../modules/MinecraftModule.js');
const simpleGit = require('simple-git');
const path = require('path');

class BackupModule extends Module {

    main: Main;

    constructor(main : Main) {
        super(main);
        (this.main : Main) = main;
    }

    listeners = {};

    mcInstance = null;

    onLoad() {
        if((this.main : Main)['repository'] === undefined){
            let folder = path.resolve((this.main : Main).getConfigs()['BACKUP']['server_path']);
            let repo = simpleGit(folder);
            (this.main : Main)['repository'] = repo;
            repo.cwd(folder);
            repo.checkIsRepo('root').then((res)=>{
                if(!res){
                    repo.init().then(()=>this.makeBackup()).catch(console.error);
                }
            }).catch(console.error);

            if((this.main : Main).getConfigs()['BACKUP']['backup_time']>0 && (this.main : Main).getConfigs()['BACKUP']['backup_time']>(this.main : Main).getConfigs()['BACKUP']['backup_alert']*1.5) {
                setImmediate(() => {
                    this.mcInstance = (this.main : Main)['MinecraftServer'];
                    (this.main : Main)['MinecraftServer'].on('start', this.listeners['start'] = () => {
                        let backingUp : boolean = false;
                        this.listeners['interval'] = setInterval(() => {
                            if(!backingUp && (backingUp=true)) {
                                (this.main: Main)['MinecraftServer'].exec('say Backup in ' + (this.main: Main).getConfigs()['BACKUP']['backup_alert'] + ' seconds, if flying please land');
                                setTimeout(async () => {
                                    await (this.main: Main)['MinecraftServer'].exec('say Backing up');
                                    await (this.main: Main)['MinecraftServer'].exec('save-off');
                                    await (this.main: Main)['MinecraftServer'].exec('save-all');
                                    let res = await this.makeBackup().catch(console.error);
                                    await (this.main: Main)['MinecraftServer'].exec('save-on');
                                    await (this.main: Main)['MinecraftServer'].exec('say Backup complete, id:' + res.commit);
                                    backingUp=false;
                                }, (this.main: Main).getConfigs()['BACKUP']['backup_alert'] * 1000);
                            }
                        }, (this.main : Main).getConfigs()['BACKUP']['backup_time'] * 1000);
                    });

                    (this.main : Main)['MinecraftServer'].on('stop', this.listeners['stop'] = () => {
                        clearInterval(this.listeners['interval']);
                        this.makeBackup().catch(console.error);
                    });

                    (this.main : Main).on('reload', this.listeners['reload'] = (old_module, new_module) => {
                        if (old_module === this.mcInstance)
                            this.mcInstance = new_module;

                        new_module.on('start', this.listeners['start']);
                        new_module.on('stop', this.listeners['stop']);
                    });
                });
            }
        }

        (this.main : Main)['BackupModule'] = this;
    }

    async makeBackup(msg=new Date().toLocaleString()){
        let repo = this.getRepository();
        await repo.add('.').catch(console.error);
        return repo.commit(msg, ['.'], ['--author="Backup <Backup@localhost>"', '--allow-empty']).catch(console.error);
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
        (this.main : Main)['MinecraftServer'].removeListener('start',this.listeners['start']);
        (this.main : Main)['MinecraftServer'].removeListener('stop',this.listeners['stop']);
        clearInterval(this.listeners['interval']);
        (this.main : Main).removelistener('reload',this.listeners['reload']);
    }

    getRepository(){
        return (this.main : Main)['repository'];
    }

}

module.exports = BackupModule;