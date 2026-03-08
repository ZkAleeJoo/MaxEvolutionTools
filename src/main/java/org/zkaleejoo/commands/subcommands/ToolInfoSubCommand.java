package org.zkaleejoo.commands.subcommands;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.utils.MessageUtils;

public class ToolInfoSubCommand implements SubCommand {

    private final MaxEvolutionTools plugin;

    public ToolInfoSubCommand(MaxEvolutionTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "toolinfo";
    }

    @Override
    public String getPermission() {
        return "maxevolutiontools.toolinfo";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayers()));
            return true;
        }

        ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
        if (!plugin.getToolEvolutionManager().isTrackedTool(item)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInvalidTool()));
            return true;
        }

        int usage = plugin.getToolEvolutionManager().getUsage(item);
        boolean special = plugin.getToolEvolutionManager().isSpecialUnlocked(item);

        String message = plugin.getConfigManager().getMsgToolInfo()
                .replace("%usage%", String.valueOf(usage))
                .replace("%special%", special ? plugin.getConfigManager().getMsgEnabledWord() : plugin.getConfigManager().getMsgDisabledWord());

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}