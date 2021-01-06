const Main = require('./interfaces/Main.js');
const Module = require('./interfaces/Module.js');
const config = require('../../configs/config.json');


const fs = require("fs");
const path = require('path');


//log errors to file
try {
    fs.mkdirSync('./logs/');
}catch (e){}
const errorWriter = fs.createWriteStream('./logs/error.log',{flags:'a'});
process.stderr.pipe(errorWriter);

class Program extends Main {

    modules = {};

    getConfigs() {
        return config;
    }

    onStart() {
        this.loadModules();
    }

    onStop() {
        this.modules.forEach(function (mod){
            mod.onUnload();
        })
    }

    reloadModule(module) {
        let file = path.resolve("./bin/scripts/modules") + "/" + module +".js";
        let oldModule: Module= this.modules[file];
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
                    file = path.resolve("./bin/scripts/modules") + "/" + file;
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