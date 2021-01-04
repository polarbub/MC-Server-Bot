const Main = require('./Main.js');

class Module
{
    onLoad(){};

    onUnload(){};

    constructor(main : Main){};
}

module.exports = Module;
