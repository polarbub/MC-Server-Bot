package net.polarbub.botv2;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.util.List;
import java.util.Objects;

import static net.polarbub.botv2.config.config.*;

public class permissions {

    //Permissions grabber
    public static boolean getPermissions(String permission, MessageReceivedEvent event) {
        String id = event.getAuthor().getId();
        List userRoles = Objects.requireNonNull(event.getMember()).getRoles();
        Guild guild = event.getGuild();

        int useNumber = 0;
        String check = permissionsConfig.yamlMapping("Global").yamlSequence("Users").string(useNumber);
        while(check != null) {
            if(check.equals(id)) {
                return true;
            }
            useNumber++;
            check = permissionsConfig.yamlMapping("Global").yamlSequence("Users").string(useNumber);
        }

        useNumber = 0;
        long checkLong;
        Role role;
        check = permissionsConfig.yamlMapping("Global").yamlSequence("Roles").string(useNumber);
        if(!(check.length() == 2 || check.equals("null"))) {
            checkLong = Long.parseLong(check);
            role = guild.getRoleById(checkLong);
            while(check.length() != 2) {
                role = guild.getRoleById(checkLong);
                if(userRoles.contains(role)) {
                    return true;
                }
                useNumber++;
                check = permissionsConfig.yamlMapping("Global").yamlSequence("Roles").string(useNumber);
                checkLong = Long.parseLong(check);
            }
        }

        useNumber = 0;
        check = permissionsConfig.yamlMapping(permission).yamlSequence("Users").string(useNumber);
        while(check != null) {
            if(check.equals(id)) {
                return true;
            }
            useNumber++;
            check = permissionsConfig.yamlMapping(permission).yamlSequence("Users").string(useNumber);
        }

        useNumber = 0;
        check = permissionsConfig.yamlMapping(permission).yamlSequence("Roles").string(useNumber);
        if(check.length() == 2 || check.equals("null")) {} else {
            checkLong = Long.parseLong(check);
            role = guild.getRoleById(checkLong);
            while(check.length() != 2) {
                role = guild.getRoleById(checkLong);
                if(userRoles.contains(role)) {
                    return true;
                }
                useNumber++;
                check = permissionsConfig.yamlMapping(permission).yamlSequence("Roles").string(useNumber);
                checkLong = Long.parseLong(check);
            }
        }
        return false;
    }

}