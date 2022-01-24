package net.polarbub.botv2;

import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Objects;

import static net.polarbub.botv2.config.config.permissionsConfig;

public class permissions {

    //Permissions grabber
    public static boolean getPermissions(String permission, MessageReceivedEvent event) {
        String id = event.getAuthor().getId();
        List<Role> userRoles = Objects.requireNonNull(event.getMember()).getRoles();


        int i = 0;
        YamlSequence yamlSequence = permissionsConfig.yamlMapping("Global").yamlSequence("Users");
        for(YamlNode node : yamlSequence) {
            if(id.equals(yamlSequence.string(i))) return true;
            i++;
        }

        i = 0;
        yamlSequence = permissionsConfig.yamlMapping(permission).yamlSequence("Roles");
        for(YamlNode node : yamlSequence) {
            for (Role role : userRoles) {
                if(role.getId().equals(yamlSequence.string(i))) return true;
            }
            i++;
        }

        i = 0;
        yamlSequence = permissionsConfig.yamlMapping("Global").yamlSequence("Roles");
        for(YamlNode node : yamlSequence) {
            for (Role role : userRoles) {
                if(role.getId().equals(yamlSequence.string(i))) return true;
            }
            i++;
        }

        i = 0;
        yamlSequence = permissionsConfig.yamlMapping(permission).yamlSequence("Users");
        for(YamlNode node : yamlSequence) {
            if(id.equals(yamlSequence.string(i))) return true;
            i++;
        }
        return false;
    }

}