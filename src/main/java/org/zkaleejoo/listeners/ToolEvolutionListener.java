package org.zkaleejoo.listeners;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
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

            if (milestone.specialUnlock()) {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgSpecialUnlocked()));
            } else {
                String message = plugin.getConfigManager().getMsgMilestoneReached()
                        .replace("%blocks%", String.valueOf(milestone.blocksRequired()))
                        .replace("%enchant%", milestone.enchantment())
                        .replace("%level%", String.valueOf(milestone.level()));
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
            }
        }

        if (evolutionManager.isSpecialUnlocked(tool)) {
            applySpecialRepair(tool);
        }
    }

    private void applySpecialRepair(ItemStack tool) {
        if (Math.random() > evolutionManager.getSpecialRepairChance()) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int damage = damageable.getDamage();
        if (damage <= 0) {
            return;
        }

        damageable.setDamage(damage - 1);
        tool.setItemMeta(meta);
    }
}