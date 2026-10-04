package com.nexuscraft.nexuscrafting;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

/**
 * One secret, mastery-gated recipe: not a brand-new item (see {@link SecretRecipeCatalog}'s own
 * class comment for why), just a genuinely real, registered {@code Recipe} a player only ever
 * sees once they've reached {@link #unlockLevel()} crafting mastery -- a cheaper or stranger path
 * to a real vanilla result than the book-standard recipe, discovered through practice rather than
 * picked up by holding the right ingredient the way an ordinary vanilla recipe unlocks.
 *
 * @param id the recipe's own id, used verbatim as the real {@code NamespacedKey}'s key string
 * @param unlockLevel the crafting-mastery level at which {@link RecipeDiscoveryListener} grants it
 * @param recipeBuilder builds the real, keyed {@code Recipe} once a {@code Plugin} is available
 *                       (registration happens once, at plugin enable -- see {@code
 *                       SecretRecipeCatalog#registerAll})
 * @param flavorText shown to the player the moment they unlock it
 */
record SecretRecipeDefinition(String id, int unlockLevel, Function<NamespacedKey, Recipe> recipeBuilder,
                               String flavorText) {

    NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, id);
    }

    Recipe build(Plugin plugin) {
        return recipeBuilder.apply(key(plugin));
    }
}
