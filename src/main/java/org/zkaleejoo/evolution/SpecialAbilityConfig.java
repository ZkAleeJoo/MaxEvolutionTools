package org.zkaleejoo.evolution;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Material;

public record SpecialAbilityConfig(
        String id,
        AbilityType type,
        boolean enabled,
        double chance,
        int amount,
        int cooldownSeconds,
        boolean compatibleWithMending,
        Set<Material> materialWhitelist
) {

    public SpecialAbilityConfig {
        materialWhitelist = materialWhitelist == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(materialWhitelist));
    }

    public boolean hasMaterialWhitelist() {
        return !materialWhitelist.isEmpty();
    }
}