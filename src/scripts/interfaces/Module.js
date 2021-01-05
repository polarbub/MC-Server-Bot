const Main = require('./Main.js');
const EventEmitter = require('events');


class Module extends EventEmitter
{
    onLoad(){};

    onUnload(){};

    constructor(main){
        super();
    };
}

module.exports = Module;
