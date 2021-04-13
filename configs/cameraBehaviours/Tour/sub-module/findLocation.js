let path = require('path');

module.exports = (function(){
    function FindLocation(bot,targets)
    {
        this.bot = bot;
        this.active = false;
        this.stateName = 'findLocation';
        this.targets = targets;
    }

    FindLocation.prototype.onStateEntered = function () {
        let file = path.resolve("./configs/locations.json");
        delete require.cache[file];
        let list = require(file);
        if(list !== undefined){
            let dest = list[Math.floor(Math.random() * list.length)];
            this.targets.teleport = dest;
            console.log(`[Camera Bot] found teleport destination: ${dest.name}`);
        }
    };

    FindLocation.prototype.onStateExited = function () {

    };

    return FindLocation;
}());