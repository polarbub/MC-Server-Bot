const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const MinecraftServer = require('../modules/MinecraftModule.js');
const simpleGit = require('simple-git');
const path = require('path');

class BackupModule extends Module {

    main: Main;

    constructor(main: Main) {
        super(main);
        (this.main: Main) = main;
    }

    listeners = {};

    mcInstance = null;

    runningGit = false;

    onLoad() {
        if ((this.main: Main)['repository'] === undefined) {
            let folder = path.resolve((this.main: Main).getConfigs()['BACKUP']['server_path']);
            let repo = simpleGit(folder);
            (this.main: Main)['repository'] = repo;
            repo.cwd(folder);
            repo.checkIsRepo('root').then((res) => {
                if (!res) {
                    this.runningGit = true;
                    repo.init().then(() => this.makeBackup()).catch(console.error);
                }
            }).catch(console.error);
        }

        if ((this.main: Main).getConfigs()['BACKUP']['backup_time'] > 0 && (this.main: Main).getConfigs()['BACKUP']['backup_time'] > (this.main: Main).getConfigs()['BACKUP']['backup_alert'] * 1.5) {
            setImmediate(() => {
                this.mcInstance = (this.main: Main)['MinecraftServer'];
                if ((this.main: Main)['server'] != null) {
                    this.onServerRunning();
                }
                this.listeners['start'] = this.onServerRunning.bind(this);
                (this.main: Main)['MinecraftServer'].on('start', this.listeners['start']);

                (this.main: Main)['MinecraftServer'].on('stop', this.listeners['stop'] = () => {
                    clearInterval(this.listeners['interval']);
                    this.makeBackup("Server Stopped").catch(console.error);
                });

                (this.main: Main).on('reload', this.listeners['reload'] = (old_module, new_module) => {
                    if (old_module === this.mcInstance) {
                        this.mcInstance = new_module;
                        if(new_module !== null) {
                            new_module.on('start', this.listeners['start']);
                            new_module.on('stop', this.listeners['stop']);
                        }
                    }
                });
            });
        }

        (this.main: Main)['BackupModule'] = this;
    }


    backingUp: boolean = false;

    onServerRunning = () => {
        this.listeners['interval'] = setInterval(() => {
            if (!(this.backingUp || this.runningGit) && (this.backingUp = true)) {
                (this.main: Main)['MinecraftServer'].exec('say Backup in ' + (this.main: Main).getConfigs()['BACKUP']['backup_alert'] + ' seconds, if flying please land');
                setTimeout(this.makeServerBackup.bind(this), (this.main: Main).getConfigs()['BACKUP']['backup_alert'] * 1000);
            }
        }, (this.main: Main).getConfigs()['BACKUP']['backup_time'] * 1000);
    };

    async makeServerBackup(msg) {
        let res
        try {
            if((this.main : Main)['server']) {
                await (this.main: Main)['MinecraftServer'].exec('say Backing up ( this might freeze the Server )');
                await (this.main: Main)['MinecraftServer'].exec('save-off');
                await new Promise(((resolve, reject) => {
                    let endRegex = new RegExp((this.main: Main).getConfigs()['MC_SERVER']['save_regex'], 'gm');
                    let waiter;
                    let timeout = setTimeout(() => {
                        (this.main: Main)['server']?.stdout.removeListener('data', waiter);
                        reject();
                    }, 60000);
                    (this.main: Main)['server'].stdout.on('data', waiter = (data: string) => {
                        if (endRegex.test(data)) {
                            (this.main: Main)['server'].stdout.removeListener('data', waiter);
                            clearTimeout(timeout);
                            resolve();
                        }
                    });
                    (this.main: Main)['MinecraftServer'].exec('save-all flush');
                }))
                res = await this.makeBackup(msg).catch(console.error);
                await (this.main: Main)['MinecraftServer'].exec('save-on');
                await (this.main: Main)['MinecraftServer'].exec('say Backup complete, id:' + res?.commit);
            }
        } catch (e) {
            console.error(e);
        }
        this.backingUp = false;
        return res;
    }

    async makeBackup(msg = "AutoBackup") {
        let repo = this.getRepository();
        this.runningGit = true;
        await repo.add(['--ignore-errors', '.']).catch(console.error);
        let ret = await repo.commit(msg, ['--author="Backup <Backup@localhost>"', '--allow-empty']).catch(console.error);
        this.runningGit = false;
        return ret;
    }

    async getBackups(count) {
        let repo = this.getRepository();
        let res = await repo.log(['HEAD']).catch(console.error)
        let list = res?.all;
        list?.sort((a, b) => {
            return -a.date.localeCompare(b.date)
        });
        list = list?.slice(0, count || list.length);
        return list;
    }

    async restoreBackup(backup = 'latest') {
        let repo = this.getRepository();
        let res = await repo.log().catch(console.error)
        let backObj = null;
        if (backup === "latest") {
            backObj = res.latest;
        } else {
            backObj = res.all.find((obj) => {
                if (obj.string === backup)
                    return true;
                if (obj.hash === backup)
                    return true;
                if (obj.date === backup)
                    return true;
                if (obj.message === backup)
                    return true;
                return false;
            });
        }

        if (backObj !== null) {
            let [month, date, year] = new Date().toLocaleDateString("en-US").split("/");
            let [hour, minute, second] = new Date().toLocaleTimeString("en-US").split(/:| /);
            let branch = `rollback--${month}-${date}-${year}--${hour}-${minute}`;
            await repo.branch(['-C', branch]);
            let reset = await repo.reset('hard', [backObj.hash]);
            return [branch, reset];
        }
    }

    getShortHash(hash) {
        let repo = this.getRepository();
        return repo.revparse(['--short', hash]);
    }

    onUnload() {
        (this.main: Main)['MinecraftServer'].removeListener('start', this.listeners['start']);
        (this.main: Main)['MinecraftServer'].removeListener('stop', this.listeners['stop']);
        clearInterval(this.listeners['interval']);
        (this.main: Main).removeListener('reload', this.listeners['reload']);
    }

    getRepository() {
        return (this.main: Main)['repository'];
    }

}

module.exports = BackupModule;