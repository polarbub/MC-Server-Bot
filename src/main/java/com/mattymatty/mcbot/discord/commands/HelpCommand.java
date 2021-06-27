package com.mattymatty.mcbot.discord.commands;

import com.mattymatty.mcbot.discord.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class HelpCommand implements Command{
    private Bot bot;

    public HelpCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "lists the bot commands";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help for " + getName() + ":");
        eb.addField(getName()," lists the bot commands",false);
        eb.addField(getName() + " [command]","shows the help for the specific command",false);
        return eb.build();
    }

    @Override
    public CommandData getCommand() {
        return new CommandData(getName(),getDescription())
                .addOption(STRING,"command","command");
    }

    @Override
    public void run(SlashCommandEvent event) {
        event.deferReply(false).queue();
        OptionMapping opt = event.getOption("command");
        if(opt == null) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Available Commands:");
            bot.commandMap.values().forEach(c -> {
                embed.addField(c.getName(), c.getDescription(), false);
            });
            event.getHook().setEphemeral(false).sendMessageEmbeds(embed.build()).queue();
        }else{
            Command cmd = bot.commandMap.get(opt.getAsString());
            if(cmd != null){
                event.getHook().setEphemeral(false).sendMessageEmbeds(cmd.getHelp()).queue();
            }
        }
    }
}
