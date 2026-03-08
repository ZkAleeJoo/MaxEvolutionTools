package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.utils.MessageUtils;

public class ReloadSubCommand implements SubCommand {

    private final MaxEvolutionTools plugin;

    public ReloadSubCommand(MaxEvolutionTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "maxevolutiontools.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfig();
        plugin.getToolEvolutionManager().reload();
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgPluginReload()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}