const EventEmitter = require('events');

class Main extends EventEmitter
{

    getConfigs(){};

    onStart(){};

    onStop(){};

    reloadModule(module : string){};

    reload(){};
}

module.exports = Main;