const EventEmitter = require('events');

class Main extends EventEmitter{

    getConfigs(){};

    onStart(){};

    onStop(){};

    reloadModule(module : string){
        this.emit('reload',null,null);
    };

    reload(){};
}

module.exports = Main;