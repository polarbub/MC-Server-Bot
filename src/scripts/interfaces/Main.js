const Module = require('./Module.js')

class Main {

    getPermissions(){};

    getConfigs(){};

    onStart(){};

    onStop(){};

    reloadModule(module: Module){};

    reload(){};
}

module.exports = Main;