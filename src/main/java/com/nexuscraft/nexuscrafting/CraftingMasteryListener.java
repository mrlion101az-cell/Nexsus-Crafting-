package com.nexuscraft.nexuscrafting;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The heart of the crafting-mastery and quality-tier/critical-crafting buckets: previews a
 * mastery-scaled outcome roll onto the result slot of a real crafting-table ({@link
 * PrepareItemCraftEvent}) or smithing-table ({@link PrepareSmithingEvent}) craft before it's
 * taken, then awards XP and announces that outcome once the player actually takes it ({@link
 * CraftItemEvent}/{@link SmithItemEvent}).
 *
 * <p>The preview re-rolls every time {@link #onPrepareCraft}/{@link #onPrepareSmith} fires --
 * which, matching real vanilla, is every time the input slots change, not just once -- so what a
 * player sees sitting in the result slot is only ever a snapshot of the most recent roll. That's
 * a deliberate simplification (see README's "known, deliberate design choices" section): a real
 * player assembling ingredients slowly will see the result flicker between outcomes, exactly the
 * way a real recipe-book preview already flickers between different matched recipes as the matrix
 * changes. Whatever is showing the instant they click IS what they get -- there's no separate,
 * independent roll at take-time.
 */
final class CraftingMasteryListener implements Listener {

    private final CraftingConfig config;
    private final CraftingSkillStore skillStore;
    private final CraftQualityItemTagger tagger;
    private final RecipeDiscoveryListener recipeDiscovery;
    private final Random random = new Random();

    CraftingMasteryListener(CraftingConfig config, CraftingSkillStore skillStore, CraftQualityItemTagger tagger,
                             RecipeDiscoveryListener recipeDiscovery) {
        this.config = config;
        this.skillStore = skillStore;
        this.tagger = tagger;
        this.recipeDiscovery = recipeDiscovery;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!config.enabled || event.isRepair()) {
            return;
        }
        CraftingInventory inventory = event.getInventory();
        Recipe recipe = event.getRecipe();
        ItemStack preview = inventory == null ? null : inventory.getResult();
        if (recipe == null || preview == null || preview.getType() == Material.AIR) {
            return;
        }
        Player player = resolvePlayer(event.getView());
        if (player == null) {
            return;
        }

        int level = skillStore.levelOf(player, config);
        applyCostRefund(inventory, level);
        inventory.setResult(rollOutcome(preview, level));
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!config.enabled || event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        ItemStack taken = event.getCurrentItem();
        if (taken == null || taken.getType() == Material.AIR) {
            player.sendMessage("§cThe craft came out ruined -- you got nothing.");
            return;
        }

        int baseAmount = recipe.getResult() != null ? recipe.getResult().getAmount() : 1;
        awardOutcome(player, config.xpPerCraft, taken, baseAmount);
    }

    @EventHandler
    public void onPrepareSmith(PrepareSmithingEvent event) {
        if (!config.enabled) {
            return;
        }
        ItemStack preview = event.getResult();
        if (preview == null || preview.getType() == Material.AIR) {
            return;
        }
        Player player = resolvePlayer(event.getView());
        if (player == null) {
            return;
        }

        int level = skillStore.levelOf(player, config);
        event.setResult(rollOutcome(preview, level));
    }

    @EventHandler
    public void onSmithItem(SmithItemEvent event) {
        if (!config.enabled || event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack taken = event.getCurrentItem();
        if (taken == null || taken.getType() == Material.AIR) {
            player.sendMessage("§cThe smithing came out ruined -- you got nothing.");
            return;
        }

        awardOutcome(player, config.xpPerSmith, taken, 1);
    }

    private Player resolvePlayer(org.bukkit.inventory.InventoryView view) {
        HumanEntity who = view == null ? null : view.getPlayer();
        return who instanceof Player player ? player : null;
    }

    /** Rolls BOTCHED/CRITICAL/NORMAL for the yield, and -- independently, only for gear {@link
     *  GearCategory} classifies as equippable -- a {@link CraftQuality} tier. Returns {@code
     *  null} for a botched craft (the result slot ends up empty); never mutates {@code preview}
     *  itself, always a clone, so a recipe's own cached result {@code ItemStack} is never
     *  accidentally carried a stray quality tag or bonus amount into the NEXT, unrelated craft
     *  that happens to match the same recipe. */
    private ItemStack rollOutcome(ItemStack preview, int level) {
        double roll = random.nextDouble();
        double botch = CraftingSkill.botchChance(level, config);
        if (roll < botch) {
            return null;
        }

        ItemStack result = preview.clone();
        double critical = CraftingSkill.criticalChance(level, config);
        if (roll < botch + critical) {
            result.setAmount(result.getAmount() + 1);
        }

        if (GearCategory.of(result.getType()).isEquippable()) {
            CraftQuality quality = CraftQualityRoller.roll(level, config, random);
            tagger.tag(result, quality, config);
        }

        return result;
    }

    /** One random, already-placed, non-full ingredient slot in the matrix keeps one extra unit
     *  instead of being consumed -- see {@link CraftingSkill#costRefundChance}'s own comment. A
     *  pure bonus, independent of the BOTCHED/CRITICAL yield roll above. */
    private void applyCostRefund(CraftingInventory inventory, int level) {
        double chance = CraftingSkill.costRefundChance(level, config);
        if (chance <= 0.0 || random.nextDouble() >= chance) {
            return;
        }
        ItemStack[] matrix = inventory.getMatrix();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack slot = matrix[i];
            if (slot != null && slot.getType() != Material.AIR && slot.getAmount() < slot.getMaxStackSize()) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        int chosen = candidates.get(random.nextInt(candidates.size()));
        matrix[chosen].setAmount(matrix[chosen].getAmount() + 1);
        inventory.setMatrix(matrix);
    }

    private void awardOutcome(Player player, long baseXp, ItemStack taken, int baseAmount) {
        int levelBefore = skillStore.levelOf(player, config);
        skillStore.addXp(player, baseXp * Math.max(1, taken.getAmount()));
        int levelAfter = skillStore.levelOf(player, config);

        if (taken.getAmount() > baseAmount) {
            player.sendMessage("§a✦ Critical craft! §7You got an extra item.");
        }

        CraftQuality quality = tagger.qualityOf(taken);
        if (quality != CraftQuality.STANDARD) {
            player.sendMessage("§7Your craft turned out §f" + quality.name().toLowerCase(Locale.ROOT) + "§7!");
        }

        if (levelAfter > levelBefore) {
            player.sendMessage("§6Crafting mastery level up! §7You're now level " + levelAfter + ".");
            recipeDiscovery.onLevelReached(player, levelAfter);
        }
    }
}
