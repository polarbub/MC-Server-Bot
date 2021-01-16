const Main = require('./interfaces/Main.js');
const Module = require('./interfaces/Module.js');


const yaml = require("js-yaml");
const fs = require("fs");
const path = require('path');

class Program extends Main {

    config = {};

    modules = {};

    getConfigs() {
        return this.config;
    }

    onStart() {
        this.LoadConfig();
        this.loadModules();
    }

    LoadConfig() {
        let cfg_content = fs.readFileSync("./configs/config.yaml", 'utf8');
        this.config = yaml.load(cfg_content);
    }

    onStop() {
        this.modules.forEach(function (mod){
            mod.onUnload();
        })
    }

    reloadModule(module) {
        let file = path.resolve("./bin/scripts/modules", module +".js");
        let oldModule = this.modules[file];
        if(oldModule !== undefined) {
            oldModule.onUnload();
            delete require.cache[file];
        }
        if(fs.existsSync(file)) {
            let mod = require(file);
            let instance = new mod(this);
            instance.onLoad();
            this.modules[file] = instance;
            this.emit('reload', oldModule, instance);
        }else{
            this.emit('reload', oldModule, null);
        }
    }

    reload() {
        Object.entries(this.modules).forEach((entry : [string,Module])=>{
            entry[1].onUnload();
            delete require.cache[entry[0]];
            delete this.modules[entry[0]];
        })
        this.loadModules();
    }

    loadModules() {
        fs.readdir('./bin/scripts/modules', (err, files) => {
            if (err) {
                return console.log('Unable to scan modules directory: ' + err);
            }

            files.forEach((file) => {
                if (path.extname(file) === ".js") {
                    file = path.resolve("./bin/scripts/modules",file);
                    let mod = require(file);
                    let instance = new mod(this);
                    instance.onLoad();
                    this.modules[file] = instance;
                }
            });
        })
    }
}

let main = new Program();
main.onStart();

return;