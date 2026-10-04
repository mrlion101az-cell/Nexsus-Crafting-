package com.nexuscraft.nexuscrafting;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * NexusCrafting -- a massive overhaul of hand-crafting and recipes, picking up where NexusLode
 * (mining/tool progression) left off: putting the player back at the center of crafting and
 * cooking, this plugin goes after the single oldest, flattest ritual in the engine-overhaul
 * series so far -- clicking a crafting-table/stonecutter/loom/cartography/smithing-table result
 * slot has been the exact same instant, uniform, skill-free action since the crafting grid itself
 * was introduced. This plugin keeps every real vanilla recipe, every real vanilla ingredient, and
 * every real click recognizable -- it never replaces a real recipe, only layers real, mastery-
 * driven outcomes on top of what the player already does.
 *
 * <p>Four systems layer on top of that real crafting loop: a per-player crafting-mastery skill
 * ({@link CraftingSkill}/{@link CraftingSkillStore}) that grows from real, resolved hand-crafts
 * (a workbench {@link org.bukkit.event.inventory.CraftItemEvent} or smithing-table {@link
 * org.bukkit.event.inventory.SmithItemEvent} actually taken) and feeds three real outcome rolls
 * ({@link CraftingMasteryListener}): a chance of a CRITICAL craft (a bonus item), a chance of a
 * BOTCHED craft (the result comes out empty -- a real risk, tunable to zero in config.yml), and a
 * chance of a cost refund (one placed ingredient keeps an extra unit); a hidden tier of recipes
 * ({@link SecretRecipeCatalog}/{@link SecretRecipeStore}/{@link RecipeDiscoveryListener}) that
 * stay off a player's real recipe book until crafting mastery actually earns them, using real
 * vanilla results reached through a genuinely different, hidden ingredient combination; a real,
 * baked-in quality tier on gear ({@link CraftQuality}/{@link CraftQualityRoller}/{@link
 * CraftQualityItemTagger}) that attaches a real, flat attribute bonus to a weapon, tool, or armor
 * piece crafted at a high enough roll; and a bonus-duplicate chance at the stonecutter, loom, and
 * cartography table ({@link StationOverhaulListener}) -- three real stations that, confirmed
 * against real 1.21.4 javadoc, expose no dedicated "prepare" event of their own to hook the way
 * the crafting and smithing tables do.
 *
 * <p>Deliberately does NOT touch item durability, repair, or sharpening -- that's NexusForge's
 * own already-shipped, already-owned mechanic; does NOT invent any new {@code Material} or
 * custom item -- every secret recipe below produces a genuinely real vanilla result; and does NOT
 * gate specific loom banner patterns -- see README.md's "known, deliberate design choices" and
 * "known boundaries with other plugins" sections for the full reasoning on each. A standalone
 * plugin, no shared compiled dependency with any other sibling -- see README.md's own section for
 * the full reasoning, same convention as every other plugin in this family.
 */
public final class NexusCraftingPlugin extends JavaPlugin {

    private CraftingConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new CraftingConfig(this);
        config.load(getConfig());

        CraftingSkillStore skillStore = new CraftingSkillStore(this);
        CraftQualityItemTagger tagger = new CraftQualityItemTagger(this);
        SecretRecipeStore secretStore = new SecretRecipeStore(this);

        SecretRecipeCatalog.registerAll(this);

        RecipeDiscoveryListener recipeDiscovery = new RecipeDiscoveryListener(this, skillStore, config, secretStore);
        CraftingMasteryListener masteryListener =
                new CraftingMasteryListener(config, skillStore, tagger, recipeDiscovery);
        StationOverhaulListener stationListener = new StationOverhaulListener(config, skillStore);

        getServer().getPluginManager().registerEvents(recipeDiscovery, this);
        getServer().getPluginManager().registerEvents(masteryListener, this);
        getServer().getPluginManager().registerEvents(stationListener, this);

        var command = getCommand("nexuscrafting");
        if (command != null) {
            command.setExecutor(new CraftingCommand(skillStore, config, secretStore, this::reload));
        }

        getLogger().info("NexusCrafting enabled -- hand-crafting and recipes have been overhauled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexusCrafting disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getConfig());
    }
}
