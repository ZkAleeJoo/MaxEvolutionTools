package org.zkaleejoo.evolution;

import java.util.List;

public record EvolutionMilestone(int blocksRequired, String enchantment, int level, List<String> unlockAbilities) {
}