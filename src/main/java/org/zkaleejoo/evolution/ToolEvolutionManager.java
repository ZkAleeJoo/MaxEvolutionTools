package org.zkaleejoo.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxEvolutionTools;
import java.util.HashMap;
import java.util.Map;



public class ToolEvolutionManager {

    private static final Map<String, String> ENCHANTMENT_ALIASES = new HashMap<>();

    static {
        ENCHANTMENT_ALIASES.put("DURABILITY", "UNBREAKING");
        ENCHANTMENT_ALIASES.put("DIG_SPEED", "EFFICIENCY");
        ENCHANTMENT_ALIASES.put("LOOT_BONUS_BLOCKS", "FORTUNE");
    }

    private final MaxEvolutionTools plugin;

    private final NamespacedKey blocksMinedKey;
    private final NamespacedKey specialUnlockedKey;

    private List<Material> trackedTools = new ArrayList<>();
    private List<EvolutionMilestone> milestones = new ArrayList<>();
    private double specialRepairChance;

    public ToolEvolutionManager(MaxEvolutionTools plugin) {
        this.plugin = plugin;
        this.blocksMinedKey = new NamespacedKey(plugin, "blocks_mined");
        this.specialUnlockedKey = new NamespacedKey(plugin, "special_unlocked");
    }

    public void reload() {
        trackedTools = parseTrackedTools(plugin.getConfigManager().getEvolutionConfig());
        milestones = parseMilestones(plugin.getConfigManager().getEvolutionConfig());
        specialRepairChance = plugin.getConfigManager().getEvolutionConfig().getDouble("special-ability.repair-chance", 0.15D);
    }

    public boolean isTrackedTool(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        return trackedTools.contains(itemStack.getType());
    }

    public int incrementUsage(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return 0;
        }

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        int current = container.getOrDefault(blocksMinedKey, PersistentDataType.INTEGER, 0);
        int updated = current + 1;
        container.set(blocksMinedKey, PersistentDataType.INTEGER, updated);
        itemStack.setItemMeta(itemMeta);
        return updated;
    }

    public int getUsage(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(blocksMinedKey, PersistentDataType.INTEGER, 0);
    }

    public boolean isSpecialUnlocked(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().getOrDefault(specialUnlockedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public List<EvolutionMilestone> getReachedMilestones(int usage) {
        List<EvolutionMilestone> reached = new ArrayList<>();
        for (EvolutionMilestone milestone : milestones) {
            if (usage >= milestone.blocksRequired()) {
                reached.add(milestone);
            }
        }
        return reached;
    }

    public boolean applyMilestone(ItemStack itemStack, EvolutionMilestone milestone) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        boolean changed = false;

        if (milestone.enchantment() != null && !milestone.enchantment().isBlank()) {
            Enchantment enchantment = resolveEnchantment(milestone.enchantment());
            if (enchantment != null) {
                int currentLevel = meta.getEnchantLevel(enchantment);
                if (currentLevel < milestone.level()) {
                    meta.removeEnchant(enchantment);
                    meta.addEnchant(enchantment, milestone.level(), true);
                    changed = true;
                }
            }
        }

        if (milestone.specialUnlock()) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.getOrDefault(specialUnlockedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 0) {
                container.set(specialUnlockedKey, PersistentDataType.BYTE, (byte) 1);
                changed = true;
            }
        }

        if (changed) {
            itemStack.setItemMeta(meta);
        }

        return changed;
    }

    private Enchantment resolveEnchantment(String configuredEnchantment) {
        String normalized = configuredEnchantment.trim();
        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(normalized.toLowerCase(Locale.ROOT)));
        if (enchantment != null) {
            return enchantment;
        }

        String alias = ENCHANTMENT_ALIASES.get(normalized.toUpperCase(Locale.ROOT));
        if (alias != null) {
            enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(alias.toLowerCase(Locale.ROOT)));
            if (enchantment != null) {
                return enchantment;
            }
        }

        plugin.getLogger().warning("Invalid enchantment configured in milestone: " + configuredEnchantment);
        return null;
    }

    public double getSpecialRepairChance() {
        return specialRepairChance;
    }

    private List<Material> parseTrackedTools(FileConfiguration config) {
        List<Material> parsed = new ArrayList<>();
        for (String raw : config.getStringList("tracked-tools")) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                parsed.add(material);
            } else {
                plugin.getLogger().warning("Invalid material in tracked-tools: " + raw);
            }
        }
        return parsed;
    }

    private List<EvolutionMilestone> parseMilestones(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("milestones");
        if (section == null) {
            return Collections.emptyList();
        }

        List<EvolutionMilestone> parsed = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection milestoneSection = section.getConfigurationSection(key);
            if (milestoneSection == null) {
                continue;
            }
            int blocks = milestoneSection.getInt("blocks", -1);
            if (blocks <= 0) {
                plugin.getLogger().warning("Invalid milestone blocks value in key " + key);
                continue;
            }

            String enchantment = milestoneSection.getString("enchantment", "");
            int level = milestoneSection.getInt("level", 1);
            boolean unlockSpecial = milestoneSection.getBoolean("unlock-special", false);
            parsed.add(new EvolutionMilestone(blocks, enchantment, level, unlockSpecial));
        }

        parsed.sort(Comparator.comparingInt(EvolutionMilestone::blocksRequired));
        return parsed;
    }
}