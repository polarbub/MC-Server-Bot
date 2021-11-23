package net.polarbub.botv2;

import me.dilley.MineStat;
import net.dv8tion.jda.api.entities.Activity;
import net.polarbub.botv2.config.config;
import net.polarbub.botv2.server.server;

import static net.polarbub.botv2.config.config.*;

public class status extends Thread{
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (server.serverRunning) {
                MineStat ms = new MineStat(IP, port);
                if (ms.getCurrentPlayers() == 0) {
                    config.bot.getPresence().setActivity(Activity.playing("No one is online"));
                } else {
                    config.bot.getPresence().setActivity(Activity.playing(ms.getCurrentPlayers() + " / " + ms.getMaximumPlayers() + " Players Online"));
                }
            } else config.bot.getPresence().setActivity(Activity.playing("Server is off"));
        }
    }
}
