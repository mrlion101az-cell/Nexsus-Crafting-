package com.nexuscraft.nexuscrafting;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * A player's total crafting-mastery XP, stored directly on their own real PDC -- same PDC-tagging
 * convention this whole family already uses for per-player skill data (compare NexusLode's own
 * {@code MiningSkillStore}, whose exact shape this mirrors). Level is never stored -- always
 * derived from total XP via {@link CraftingSkill#levelFor}, so the two can't drift.
 */
final class CraftingSkillStore {

    private final NamespacedKey xpKey;

    CraftingSkillStore(Plugin plugin) {
        this.xpKey = new NamespacedKey(plugin, "crafting_xp");
    }

    long xpOf(Player player) {
        Long value = player.getPersistentDataContainer().get(xpKey, PersistentDataType.LONG);
        return value == null ? 0L : value;
    }

    int levelOf(Player player, CraftingConfig config) {
        return CraftingSkill.levelFor(xpOf(player), config);
    }

    /** Returns the new total XP. */
    long addXp(Player player, long amount) {
        long updated = xpOf(player) + amount;
        player.getPersistentDataContainer().set(xpKey, PersistentDataType.LONG, updated);
        return updated;
    }
}
