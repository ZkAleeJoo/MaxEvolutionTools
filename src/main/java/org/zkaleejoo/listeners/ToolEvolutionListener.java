package org.zkaleejoo.listeners;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;

public class ToolEvolutionListener implements Listener {

    private final MaxEvolutionTools plugin;
    private final ToolEvolutionManager evolutionManager;

    public ToolEvolutionListener(MaxEvolutionTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);

        if (!evolutionManager.isTrackedTool(tool)) {
            return;
        }

        int usage = evolutionManager.incrementUsage(tool);
        List<EvolutionMilestone> reachedMilestones = evolutionManager.getReachedMilestones(usage);

        for (EvolutionMilestone milestone : reachedMilestones) {
            boolean changed = evolutionManager.applyMilestone(tool, milestone);
            if (!changed) {
                continue;
            }

            if (milestone.enchantment() != null && !milestone.enchantment().isBlank()) {
                String message = plugin.getConfigManager().getMsgMilestoneReached()
                        .replace("%blocks%", String.valueOf(milestone.blocksRequired()))
                        .replace("%enchant%", milestone.enchantment())
                        .replace("%level%", String.valueOf(milestone.level()));
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
            }

            for (String abilityId : evolutionManager.getAbilitiesToNotify(milestone)) {
                String message = plugin.getConfigManager().getMsgSpecialUnlocked()
                        .replace("%ability%", abilityId);
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
            }
        }

        evolutionManager.processSpecialAbilities(tool);
    }
}