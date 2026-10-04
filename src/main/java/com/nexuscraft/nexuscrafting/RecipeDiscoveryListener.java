package com.nexuscraft.nexuscrafting;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.plugin.Plugin;

/**
 * Keeps a player's real recipe-book visibility for every {@link SecretRecipeCatalog} entry in
 * sync with {@link SecretRecipeStore}'s own record of what they've actually earned, and grants
 * new unlocks as crafting mastery rises.
 *
 * <p>{@link #onRecipeDiscover} exists for one real, narrow reason: real vanilla's own recipe-book
 * auto-discovery can unlock ANY registered recipe the moment a player holds a matching
 * ingredient, including one of this plugin's own still-locked secret recipes (the whole point of
 * {@code secret_lantern} is that it stays hidden until level 20, not "until you happen to pick up
 * an iron ingot"). Cancelling the real event for a secret recipe the player hasn't earned yet is
 * what actually keeps it secret; every other recipe (every real vanilla one, and one of this
 * plugin's own ALREADY-earned secrets) passes through untouched.
 */
final class RecipeDiscoveryListener implements Listener {

    private final Plugin plugin;
    private final CraftingSkillStore skillStore;
    private final CraftingConfig config;
    private final SecretRecipeStore secretStore;

    RecipeDiscoveryListener(Plugin plugin, CraftingSkillStore skillStore, CraftingConfig config,
                             SecretRecipeStore secretStore) {
        this.plugin = plugin;
        this.skillStore = skillStore;
        this.config = config;
        this.secretStore = secretStore;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int level = skillStore.levelOf(player, config);

        for (SecretRecipeDefinition definition : SecretRecipeCatalog.ALL) {
            // Auto-grant anything crafting mastery alone already qualifies for but the player
            // somehow never got flagged for (an upgrade from an earlier version, a restored
            // backup, etc.) before syncing the real recipe-book flag either way.
            if (!secretStore.hasUnlocked(player, definition) && level >= definition.unlockLevel()) {
                secretStore.markUnlocked(player, definition);
            }

            NamespacedKey key = definition.key(plugin);
            if (secretStore.hasUnlocked(player, definition)) {
                player.discoverRecipe(key);
            } else {
                player.undiscoverRecipe(key);
            }
        }
    }

    /** Called from {@link CraftingMasteryListener} the moment a craft/smith/station-use XP award
     *  pushes a player's mastery level up -- grants every secret recipe newly within reach. */
    void onLevelReached(Player player, int newLevel) {
        for (SecretRecipeDefinition definition : SecretRecipeCatalog.ALL) {
            if (secretStore.hasUnlocked(player, definition) || newLevel < definition.unlockLevel()) {
                continue;
            }
            secretStore.markUnlocked(player, definition);
            player.discoverRecipe(definition.key(plugin));
            player.sendMessage("§dYou've pieced together a secret recipe! §7" + definition.flavorText());
        }
    }

    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        SecretRecipeDefinition definition = bySecretKey(event.getRecipe());
        if (definition == null) {
            return;
        }
        if (!secretStore.hasUnlocked(event.getPlayer(), definition)) {
            event.setCancelled(true);
        }
    }

    private SecretRecipeDefinition bySecretKey(NamespacedKey discovered) {
        if (discovered == null || !discovered.getNamespace().equals(plugin.getName().toLowerCase(java.util.Locale.ROOT))) {
            return null;
        }
        return SecretRecipeCatalog.byId(discovered.getKey());
    }
}
