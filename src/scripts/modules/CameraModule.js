const Module = require('../interfaces/Module.js');
const Main = require('../interfaces/Main.js');
const Mineflayer = require('mineflayer');
const Viewer = require('prismarine-viewer')['mineflayer'];

const path = require('path');

class CameraModule extends Module {


    main: Main = null;

    constructor(main) {
        super(main);
        (this.main : Main) = main;
    }

    listeners = {};

    client : Mineflayer.Bot;

    onLoad() {
        (this.main : Main)["Camera"] = this;
        //wait next cycle to ensure all the modules are loaded
        setImmediate(() => {
            if((this.main : Main)["CameraClient"])
                this.client = (this.main : Main)["CameraClient"];

            let doneHandler = (chunk) => {
                if (chunk.includes("Done")) {
                    this.connect();
                }
            }
            (this.main : Main)['MinecraftServer'].on('start', this.listeners['start'] = () => {
                (this.main : Main)['MinecraftServer'].getServer().stdout.on('data', this.listeners['done'] = doneHandler.bind(this));
            });
        });
    }

    actions : API;

    connect (script = this.main.getConfigs()['CAMERA_ACCOUNT']['SCRIPT']) {
        if (this.client === undefined) {
            this.client = Mineflayer.createBot({
                host: (this.main: Main).getConfigs()['MC_SERVER']['ip'],
                username: this.main.getConfigs()['CAMERA_ACCOUNT']['EMAIL'],
                password: this.main.getConfigs()['CAMERA_ACCOUNT']['PASSWORD']
            });
            (this.main: Main)["CameraClient"] = this.client;

            this.client.physics.gravity = 0.0

            this.executeScript(script);

            this.client.on('spawn', this.listeners['spawn'] = () => {
                this.client?.removeListener('spawn', this.listeners['spawn']);
                Viewer(this.client, {
                    viewDistance: 6,
                    firstPerson: true,
                    port: this.main.getConfigs()['CAMERA_ACCOUNT']['PORT']
                });
            })

            this.client.on('end', this.listeners['end'] = () => {
                this.actions.stop();
                this.actions = undefined;
                this.client.viewer?.close();
                this.client.on('spawn', this.listeners['spawn']);
                (this.main: Main)["CameraClient"] = this.client = undefined;
            })
        }
    }

    executeScript(script) {
        this.actions?.stop();
        let file = path.resolve(script);
        delete require.cache[file];
        try {
            this.actions = new (require(file): API)(this.client)
        } catch (err) {
            console.error(err);
        }
    }

    onUnload() {
        let listener = this.listeners['spawn'];
        if(listener!==undefined)
            this.client?.removeListener('spawn',listener);
        listener = this.listeners['end'];
        if(listener!==undefined)
            this.client?.removeListener('end',listener);
        listener = this.listeners['start'];
        if(listener!==undefined)
            (this.main : Main)['MinecraftServer'].removeListener('start',listener);
        listener = this.listeners['done'];
        if(listener!==undefined)
            (this.main : Main)['MinecraftServer'].getServer()?.stdout?.removeListener('data',listener);
    }
}

class API {
    constructor(client : Mineflayer.Bot) {
    }

    stop(){}
}

CameraModule.API = API;

module.exports = CameraModule;