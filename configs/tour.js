const CameraModule = require('../bin/scripts/modules/CameraModule');

const Mineflayer = require('mineflayer');
const StateMachine = require('mineflayer-statemachine');
const Pathfinder = require('mineflayer-pathfinder');


class Tour extends CameraModule.API{

    server;

    constructor(bot) {
        super(bot);

        bot.loadPlugin(Pathfinder['pathfinder']);

        let spawnListener;
        bot.on('spawn',spawnListener = ()=>{
            bot.removeListener('spawn',spawnListener);

            let next = function (){
                bot.creative.startFlying()
                //TODO: add actions for the bot

                let targets={};
                let getClosestEntity = new StateMachine.BehaviorGetClosestEntity(bot, targets,StateMachine.EntityFilters().AllEntities);
                let lookAtEntity = new StateMachine.BehaviorLookAtEntity(bot, targets);
                let idle = new StateMachine.BehaviorIdle();

                let timeout = false;
                let result = false;
                let transitions = [

                    new StateMachine.StateTransition({
                        parent: getClosestEntity,
                        child: lookAtEntity,
                        name: 'Entity Found',
                        shouldTransition: () => true
                    }),

                    new StateMachine.StateTransition({
                        parent: lookAtEntity,
                        child: idle,
                        name: 'Wait',
                        shouldTransition: () => lookAtEntity.distanceToTarget() >= 2
                    }),


                    new StateMachine.StateTransition({
                        parent: idle,
                        child: getClosestEntity,
                        name: 'Find Target',
                        shouldTransition: () => {
                            if(!timeout){
                                result=false;
                                setTimeout(() => {
                                    result=true;
                                }, 500);
                            }
                            return timeout = !result;
                        }
                    }),
                ];

                let rootLayer = new StateMachine.NestedStateMachine(transitions, getClosestEntity);

                let machine = new StateMachine.BotStateMachine(bot, rootLayer);

                this.server = new StateMachine.StateMachineWebserver(bot, machine, 8081);
                this.server.startServer();
            }.bind(this);

            //undefined is spectator ( until mojang adds a new gamemode )
            if(bot.game.gameMode !== undefined){
                bot.chat('/gamemode spectator');
                setTimeout(()=>{
                    if(bot.game.gameMode !== undefined){
                        bot.chat('Error: not able to enter Spectator mode\nQuitting');
                        bot.quit('not able to enter Spectator mode');
                    }
                    next();
                },5000);
            }else {
                next();
            }
        })
    }

    stop(){
    }
}

module.exports = Tour;