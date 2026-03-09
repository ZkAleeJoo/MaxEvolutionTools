package org.zkaleejoo.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.MaxEvolutionTools;
import org.zkaleejoo.utils.MessageUtils;
import org.bukkit.inventory.ItemFlag;

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
    private final NamespacedKey unlockedAbilitiesKey;
    private final NamespacedKey managedLoreLinesKey;

    private List<Material> trackedTools = new ArrayList<>();
    private List<EvolutionMilestone> milestones = new ArrayList<>();
    private Map<String, SpecialAbilityConfig> specialAbilities = new LinkedHashMap<>();

    public ToolEvolutionManager(MaxEvolutionTools plugin) {
        this.plugin = plugin;
        this.blocksMinedKey = new NamespacedKey(plugin, "blocks_mined");
        this.specialUnlockedKey = new NamespacedKey(plugin, "special_unlocked");
        this.unlockedAbilitiesKey = new NamespacedKey(plugin, "unlocked_abilities");
        this.managedLoreLinesKey = new NamespacedKey(plugin, "managed_lore_lines");
    }

    public void reload() {
        FileConfiguration config = plugin.getConfigManager().getEvolutionConfig();
        trackedTools = parseTrackedTools(config);
        milestones = parseMilestones(config);
        specialAbilities = parseSpecialAbilities(config);
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
        return !getUnlockedAbilities(itemStack).isEmpty();
    }

    public boolean hasUnlockedAbility(ItemStack itemStack, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return false;
        }
        return getUnlockedAbilities(itemStack).contains(abilityId.trim().toLowerCase(Locale.ROOT));
    }

    public SpecialAbilityConfig getSpecialAbilityConfig(String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return null;
        }
        return specialAbilities.get(abilityId.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> getUnlockedAbilities(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return Collections.emptySet();
        }

        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
        String raw = container.getOrDefault(unlockedAbilitiesKey, PersistentDataType.STRING, "");
        Set<String> unlocked = new LinkedHashSet<>();

        if (raw != null && !raw.isBlank()) {
            for (String token : raw.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isBlank()) {
                    unlocked.add(normalized);
                }
            }
        }

        if (container.getOrDefault(specialUnlockedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1) {
            unlocked.add("self-repair");
        }

        return unlocked;
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

        if (!milestone.unlockAbilities().isEmpty()) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Set<String> unlocked = getUnlockedAbilities(itemStack);
            boolean unlockedChanged = false;
            for (String abilityId : milestone.unlockAbilities()) {
                String normalized = abilityId.toLowerCase(Locale.ROOT);
                if (specialAbilities.containsKey(normalized) && unlocked.add(normalized)) {
                    unlockedChanged = true;
                }
            }

            if (unlockedChanged) {
                container.set(unlockedAbilitiesKey, PersistentDataType.STRING, String.join(",", unlocked));
                if (unlocked.contains("self-repair")) {
                    container.set(specialUnlockedKey, PersistentDataType.BYTE, (byte) 1);
                }
                changed = true;
            }
        }

        if (changed) {
            updateManagedLore(meta);
            itemStack.setItemMeta(meta);
        }

        return changed;
    }

    public List<String> getAbilitiesToNotify(EvolutionMilestone milestone) {
        return milestone.unlockAbilities().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(specialAbilities::containsKey)
                .toList();
    }

    public void processSpecialAbilities(ItemStack tool) {
        Set<String> unlocked = getUnlockedAbilities(tool);
        if (unlocked.isEmpty()) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        for (String abilityId : unlocked) {
            SpecialAbilityConfig ability = specialAbilities.get(abilityId);
            if (ability == null || ability.type() != AbilityType.SELF_REPAIR) {
                continue;
            }

            if (!canProcAbility(meta, ability)) {
                continue;
            }

            if (applyAbility(meta, ability)) {
                markCooldown(meta, abilityId, ability.cooldownSeconds());
                changed = true;
            }
        }

        if (changed) {
            tool.setItemMeta(meta);
        }
    }

    public boolean canProcAbility(ItemMeta meta, SpecialAbilityConfig ability) {
        if (meta == null || ability == null || !ability.enabled()) {
            return false;
        }

        if (!ability.compatibleWithMending() && meta.hasEnchant(Enchantment.MENDING)) {
            return false;
        }

        if (!isOffCooldown(meta, ability.id())) {
            return false;
        }

        return Math.random() <= ability.chance();
    }

    public void applyCooldown(ItemMeta meta, SpecialAbilityConfig ability) {
        if (meta == null || ability == null) {
            return;
        }
        markCooldown(meta, ability.id(), ability.cooldownSeconds());
    }

    private boolean applyAbility(ItemMeta meta, SpecialAbilityConfig ability) {
        return switch (ability.type()) {
            case SELF_REPAIR -> applySelfRepair(meta, ability.amount());
            case AUTO_SMELT, TELEPATHY, DRILL, HAMMER, XP_BOOST, HASTE -> false;
        };
    }

    private boolean applySelfRepair(ItemMeta meta, int amount) {
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }

        int damage = damageable.getDamage();
        if (damage <= 0) {
            return false;
        }

        int repairedDamage = Math.max(0, damage - Math.max(1, amount));
        damageable.setDamage(repairedDamage);
        return true;
    }


    private boolean isOffCooldown(ItemMeta meta, String abilityId) {
        NamespacedKey cooldownKey = new NamespacedKey(plugin, "ability_cooldown_" + abilityId.replace('-', '_'));
        long now = System.currentTimeMillis();
        long nextUse = meta.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        return now >= nextUse;
    }

    private void markCooldown(ItemMeta meta, String abilityId, int cooldownSeconds) {
        NamespacedKey cooldownKey = new NamespacedKey(plugin, "ability_cooldown_" + abilityId.replace('-', '_'));
        long nextUse = System.currentTimeMillis() + Math.max(0, cooldownSeconds) * 1000L;
        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, nextUse);
    }

    private void updateManagedLore(ItemMeta meta) {
        if (!plugin.getConfigManager().isEvolutionLoreEnabled()) {
            return;
        }

        List<String> lore = new ArrayList<>();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        List<String> managedLore = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
            String rendered = plugin.getConfigManager().getEvolutionLoreLineFormat()
                    .replace("{enchant}", getDisplayEnchantmentName(enchant.getKey()))
                    .replace("{key}", enchant.getKey().getKey().getKey())
                    .replace("{level}", String.valueOf(enchant.getValue()));
            managedLore.add(MessageUtils.getColoredMessage(rendered));
        }

        if (!managedLore.isEmpty()) {
            lore.addAll(managedLore);
            container.set(managedLoreLinesKey, PersistentDataType.STRING, String.join("\n", managedLore));
        } else {
            container.remove(managedLoreLinesKey);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        meta.setLore(lore.isEmpty() ? null : lore);
    }

    public String getDisplayEnchantmentName(String configuredEnchantment) {
        Enchantment enchantment = resolveEnchantment(configuredEnchantment);
        if (enchantment == null) {
            return configuredEnchantment;
        }
        return getDisplayEnchantmentName(enchantment);
    }

    private String getDisplayEnchantmentName(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        String translated = plugin.getConfigManager().getEnchantmentName(key);
        if (!translated.isBlank()) {
            return translated;
        }

        String[] parts = key.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
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

    private List<Material> parseTrackedTools(FileConfiguration config) {
        List<Material> parsed = new ArrayList<>();
        for (String raw : config.getStringList("tracked-tools")) {
            try {
                Material material = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                if (!parsed.contains(material)) {
                    parsed.add(material);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid material in tracked-tools: " + raw + " (use exact Material enum names)");
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
            int level = Math.max(1, milestoneSection.getInt("level", 1));
            List<String> unlockAbilities = milestoneSection.getStringList("unlock-abilities");

            if (unlockAbilities.isEmpty() && milestoneSection.getBoolean("unlock-special", false)) {
                unlockAbilities = List.of("self-repair");
            }

            parsed.add(new EvolutionMilestone(blocks, enchantment, level, normalizeAbilityIds(unlockAbilities)));
        }

        parsed.sort(Comparator.comparingInt(EvolutionMilestone::blocksRequired));
        return parsed;
    }

    private Map<String, SpecialAbilityConfig> parseSpecialAbilities(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("special-abilities");
        Map<String, SpecialAbilityConfig> parsed = new LinkedHashMap<>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection abilitySection = section.getConfigurationSection(key);
                if (abilitySection == null) {
                    continue;
                }

                SpecialAbilityConfig ability = parseSingleAbility(key, abilitySection);
                if (ability != null) {
                    parsed.put(key.toLowerCase(Locale.ROOT), ability);
                }
            }
        }

        if (parsed.isEmpty()) {
            double legacyChance = config.getDouble("special-ability.repair-chance", 0.15D);
            parsed.put("self-repair", new SpecialAbilityConfig(
                    "self-repair",
                    AbilityType.SELF_REPAIR,
                    true,
                    clampChance(legacyChance, "special-ability.repair-chance"),
                    1,
                    0,
                    true));
        }

        return parsed;
    }

    private SpecialAbilityConfig parseSingleAbility(String id, ConfigurationSection section) {
        String typeRaw = section.getString("type", id).toUpperCase(Locale.ROOT).replace('-', '_');
        AbilityType type;
        try {
            type = AbilityType.valueOf(typeRaw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid ability type for " + id + ": " + typeRaw);
            return null;
        }

        boolean enabled = section.getBoolean("enabled", true);
        double chance = clampChance(section.getDouble("chance", 0.15D), "special-abilities." + id + ".chance");
        int amount = Math.max(1, section.getInt("amount", 1));
        int cooldownSeconds = Math.max(0, section.getInt("cooldown-seconds", 0));
        boolean compatibleWithMending = section.getBoolean("compatible-with-mending", true);

        return new SpecialAbilityConfig(id.toLowerCase(Locale.ROOT), type, enabled, chance, amount, cooldownSeconds, compatibleWithMending);
    }

    private List<String> normalizeAbilityIds(List<String> ids) {
        return ids.stream()
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .filter(id -> !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private double clampChance(double raw, String path) {
        if (raw < 0.0D) {
            plugin.getLogger().warning("Configured chance is below 0.0 at " + path + ". Clamping to 0.0");
            return 0.0D;
        }
        if (raw > 1.0D) {
            plugin.getLogger().warning("Configured chance is above 1.0 at " + path + ". Clamping to 1.0");
            return 1.0D;
        }
        return raw;
    }
}