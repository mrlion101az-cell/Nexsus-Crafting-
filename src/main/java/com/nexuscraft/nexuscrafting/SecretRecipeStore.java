package com.nexuscraft.nexuscrafting;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Which {@link SecretRecipeDefinition}s a player has personally earned -- this plugin's own
 * source of truth, PDC-tagged on the player (one boolean key per recipe id, same per-fact-PDC-key
 * convention this family already uses rather than packing several facts into one delimited
 * string). Deliberately separate from real Bukkit's own {@code HumanEntity#hasDiscoveredRecipe} --
 * that's the real client-visible recipe-book flag {@link RecipeDiscoveryListener} drives FROM
 * this store on join/unlock, not a second place this plugin reads its own earned state back from
 * (a player's recipe-book state isn't guaranteed to survive every real edge case -- an offline
 * edit, a resource-pack recipe-book reset -- the way this plugin's own PDC-backed record does).
 */
final class SecretRecipeStore {

    private final Plugin plugin;

    SecretRecipeStore(Plugin plugin) {
        this.plugin = plugin;
    }

    boolean hasUnlocked(Player player, SecretRecipeDefinition definition) {
        NamespacedKey key = earnedKey(definition);
        Boolean value = player.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN);
        return value != null && value;
    }

    void markUnlocked(Player player, SecretRecipeDefinition definition) {
        player.getPersistentDataContainer().set(earnedKey(definition), PersistentDataType.BOOLEAN, true);
    }

    private NamespacedKey earnedKey(SecretRecipeDefinition definition) {
        return new NamespacedKey(plugin, "secret_earned_" + definition.id());
    }
}
