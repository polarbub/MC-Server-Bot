package net.polarbub.botv2;

import me.dilley.MineStat;
import net.dv8tion.jda.api.entities.Activity;
import static net.polarbub.botv2.config.*;

public class status extends Thread{
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (Main.serverRunning) {
                MineStat ms = new MineStat(IP, port);
                if (ms.getCurrentPlayers() == 0) {
                    Main.bot.getPresence().setActivity(Activity.playing("No one is online"));
                } else {
                    Main.bot.getPresence().setActivity(Activity.playing(ms.getCurrentPlayers() + " / " + ms.getMaximumPlayers() + " Players Online"));
                }
            } else Main.bot.getPresence().setActivity(Activity.playing("Server is off"));
        }
    }
}
