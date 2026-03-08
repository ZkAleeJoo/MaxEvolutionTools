package org.zkaleejoo.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.utils.MessageUtils;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.zkaleejoo.commands.subcommands.ReloadSubCommand;
import org.zkaleejoo.commands.subcommands.SubCommand;
import org.zkaleejoo.commands.subcommands.ToolInfoSubCommand;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final MaxEvolutionTools plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public MainCommand(MaxEvolutionTools plugin) {
        this.plugin = plugin;
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new ToolInfoSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
    }

    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgCommandUsage()));
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgCommandUsage()));
            return true;
        }
        if (!subCommand.getPermission().isBlank() && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
            return true;
        }

        return subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }




    //TAB COMPLETION
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            List<String> rootCompletions = new ArrayList<>();
            for (SubCommand subCommand : subCommands.values()) {
                if (subCommand.getPermission().isBlank() || sender.hasPermission(subCommand.getPermission())) {
                    rootCompletions.add(subCommand.getName());
                }
            }
            return filterCompletions(rootCompletions, args[0]);
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return new ArrayList<>();
        }

        if (!subCommand.getPermission().isBlank() && !sender.hasPermission(subCommand.getPermission())) {
            return new ArrayList<>();
        }

        return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }


    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                filtered.add(completion);
            }
        }
        return filtered;
    }



}