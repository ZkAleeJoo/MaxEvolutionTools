package org.zkaleejoo.evolution;

public record SpecialAbilityConfig(
        String id,
        AbilityType type,
        boolean enabled,
        double chance,
        int amount,
        int cooldownSeconds,
        boolean compatibleWithMending
) {
}