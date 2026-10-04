package com.nexuscraft.nexuscrafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * The fixed set of secret, mastery-gated recipes this version of NexusCrafting ships with --
 * deliberately NOT brand-new items (this project doesn't invent its own custom {@code Material}
 * or stake out ownership of a new one another sibling might also want to claim), just genuinely
 * real vanilla RESULTS reached through a genuinely different, hidden, usually cheaper real
 * ingredient combination -- the "a seasoned crafter knows a shortcut a beginner doesn't" feeling,
 * same restraint this family already shows elsewhere (compare NexusLode's own choice not to
 * invent new ore types). Every result material and every ingredient material used below already
 * exists, real, in this project's target game version.
 */
final class SecretRecipeCatalog {

    private SecretRecipeCatalog() {
    }

    static final List<SecretRecipeDefinition> ALL = List.of(
            new SecretRecipeDefinition(
                    "secret_spyglass",
                    10,
                    key -> new ShapelessRecipe(key, new ItemStack(Material.SPYGLASS))
                            .addIngredient(new RecipeChoice.MaterialChoice(Material.COPPER_INGOT))
                            .addIngredient(new RecipeChoice.MaterialChoice(Material.GLASS)),
                    "§7A lens-grinder's shortcut -- a spyglass from a single copper ingot and a pane of glass, no amethyst needed."
            ),
            new SecretRecipeDefinition(
                    "secret_bell",
                    15,
                    key -> new ShapelessRecipe(key, new ItemStack(Material.BELL))
                            .addIngredient(4, Material.IRON_INGOT)
                            .addIngredient(new RecipeChoice.MaterialChoice(Material.REDSTONE)),
                    "§7A founder's trick -- cast a bell from iron ingots alone, no iron block required."
            ),
            new SecretRecipeDefinition(
                    "secret_lantern",
                    20,
                    key -> new ShapelessRecipe(key, new ItemStack(Material.LANTERN))
                            .addIngredient(new RecipeChoice.MaterialChoice(Material.IRON_INGOT))
                            .addIngredient(new RecipeChoice.MaterialChoice(Material.GLOWSTONE_DUST)),
                    "§7A lamplighter's secret -- an everburning lantern from one ingot and a pinch of glowstone dust."
            )
    );

    static SecretRecipeDefinition byId(String id) {
        for (SecretRecipeDefinition definition : ALL) {
            if (definition.id().equals(id)) {
                return definition;
            }
        }
        return null;
    }

    /** Registers every secret recipe with the real, server-wide recipe registry -- once, at
     *  plugin enable. Per-player VISIBILITY (whether a given player has actually discovered one
     *  yet) is separate, handled by {@link RecipeDiscoveryListener}/{@link SecretRecipeStore} --
     *  a registered-but-undiscovered recipe is real and craftable by someone who already knows
     *  the combination, but won't show up in an undiscovered player's own recipe book. */
    static void registerAll(Plugin plugin) {
        for (SecretRecipeDefinition definition : ALL) {
            Recipe recipe = definition.build(plugin);
            Bukkit.addRecipe(recipe);
        }
    }
}
