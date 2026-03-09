package org.zkaleejoo.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.evolution.EvolutionMilestone;
import org.zkaleejoo.evolution.SpecialAbilityConfig;
import org.zkaleejoo.evolution.ToolEvolutionManager;
import org.zkaleejoo.utils.MessageUtils;

public class ToolEvolutionListener implements Listener {

    private final MaxEvolutionTools plugin;
    private final ToolEvolutionManager evolutionManager;
    private final Set<UUID> drillProcessingPlayers = new HashSet<>();

    public ToolEvolutionListener(MaxEvolutionTools plugin, ToolEvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
        startHasteTask();
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

        processXpBoost(event, tool);
        processDropAbilities(event, player, tool);
        processDrillAbilities(event, player, tool);
        evolutionManager.processSpecialAbilities(tool);
    }

    private void processDropAbilities(BlockBreakEvent event, Player player, ItemStack tool) {
        Block block = event.getBlock();
        ItemMeta meta = tool.getItemMeta();
        if (meta == null || block.getType() == Material.AIR) {
            return;
        }

        SpecialAbilityConfig telepathy = getActiveAbility(tool, "telepathy");
        SpecialAbilityConfig autoSmelt = getActiveAbility(tool, "auto-smelt");
        boolean telepathyProc = telepathy != null && evolutionManager.canProcAbility(meta, telepathy);
        boolean autoSmeltProc = autoSmelt != null && evolutionManager.canProcAbility(meta, autoSmelt);

        if (!telepathyProc && !autoSmeltProc) {
            return;
        }

        List<ItemStack> drops = block.getDrops(tool, player).stream()
                .map(ItemStack::clone)
                .toList();

        if (drops.isEmpty()) {
            return;
        }

        if (autoSmeltProc) {
            drops = autoSmeltDrops(drops);
            evolutionManager.applyCooldown(meta, autoSmelt);
        }

        event.setDropItems(false);
        if (telepathyProc) {
            evolutionManager.applyCooldown(meta, telepathy);
            for (ItemStack drop : drops) {
                player.getInventory().addItem(drop).values().forEach(leftover ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        } else {
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        tool.setItemMeta(meta);
    }

    private void processXpBoost(BlockBreakEvent event, ItemStack tool) {
        SpecialAbilityConfig xpBoost = getActiveAbility(tool, "xp-boost");
        if (xpBoost == null) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !evolutionManager.canProcAbility(meta, xpBoost)) {
            return;
        }

        if (xpBoost.hasMaterialWhitelist() && !xpBoost.materialWhitelist().contains(event.getBlock().getType())) {
            return;
        }

        int baseXp = event.getExpToDrop();
        if (baseXp <= 0) {
            return;
        }

        int multiplier = Math.max(1, xpBoost.amount());
        event.setExpToDrop(baseXp * multiplier);
        evolutionManager.applyCooldown(meta, xpBoost);
        tool.setItemMeta(meta);
    }

    private void processDrillAbilities(BlockBreakEvent event, Player player, ItemStack tool) {
        UUID playerId = player.getUniqueId();
        if (drillProcessingPlayers.contains(playerId)) {
            return;
        }

        boolean isPickaxe = tool.getType().name().endsWith("_PICKAXE");
        boolean isShovel = tool.getType().name().endsWith("_SHOVEL");

        SpecialAbilityConfig drill = isPickaxe ? getActiveAbility(tool, "drill") : null;
        SpecialAbilityConfig hammer = isShovel ? getActiveAbility(tool, "hammer") : null;
        SpecialAbilityConfig ability = drill != null ? drill : hammer;

        if (ability == null || !isHighTierTool(tool.getType())) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !evolutionManager.canProcAbility(meta, ability)) {
            return;
        }

        List<Block> targets = getThreeByThreeTargets(event.getBlock(), player, Math.max(1, ability.amount()));
        if (targets.isEmpty()) {
            return;
        }

        evolutionManager.applyCooldown(meta, ability);
        tool.setItemMeta(meta);

        drillProcessingPlayers.add(playerId);
        try {
            for (Block target : targets) {
                if (target.equals(event.getBlock()) || target.getType() == Material.AIR) {
                    continue;
                }
                if (target.isPreferredTool(tool)) {
                    target.breakNaturally(tool);
                }
            }
        } finally {
            drillProcessingPlayers.remove(playerId);
        }
    }

    private SpecialAbilityConfig getActiveAbility(ItemStack tool, String abilityId) {
        if (!evolutionManager.hasUnlockedAbility(tool, abilityId)) {
            return null;
        }
        SpecialAbilityConfig config = evolutionManager.getSpecialAbilityConfig(abilityId);
        return config != null && config.enabled() ? config : null;
    }

    private List<ItemStack> autoSmeltDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            Material smelted = switch (drop.getType()) {
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                default -> null;
            };
            if (smelted == null) {
                result.add(drop);
            } else {
                result.add(new ItemStack(smelted, drop.getAmount()));
            }
        }
        return result;
    }

    private List<Block> getThreeByThreeTargets(Block center, Player player, int radius) {
        List<Block> blocks = new ArrayList<>();
        BlockFace facing = player.getFacing();
        boolean vertical = Math.abs(player.getLocation().getPitch()) > 60;

        for (int first = -radius; first <= radius; first++) {
            for (int second = -radius; second <= radius; second++) {
                Block target;
                if (vertical) {
                    target = center.getRelative(first, 0, second);
                } else if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    target = center.getRelative(0, first, second);
                } else {
                    target = center.getRelative(first, second, 0);
                }
                blocks.add(target);
            }
        }
        return blocks;
    }

    private boolean isHighTierTool(Material material) {
        return material.name().startsWith("DIAMOND_") || material.name().startsWith("NETHERITE_");
    }

    private void startHasteTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player == null) {
                    continue;
                }
                ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);
                if (!evolutionManager.isTrackedTool(tool)) {
                    continue;
                }

                SpecialAbilityConfig haste = getActiveAbility(tool, "haste");
                if (haste == null) {
                    continue;
                }

                int amplifier = Math.max(0, haste.amount() - 1);
                PotionEffect effect = new PotionEffect(PotionEffectType.HASTE, 60, amplifier, true, false, true);
                player.addPotionEffect(effect);
            }
        }, 20L, 20L);
    }
}