package com.nexuscraft.nexuscrafting;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LoomInventory;
import org.bukkit.inventory.StonecutterInventory;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * Gives the stonecutter, loom, and cartography table a genuine new mechanical purpose: a
 * mastery-scaled chance of a bonus duplicate on whatever a player takes out of their result slot.
 * Real Bukkit, confirmed against real 1.21.4 javadoc before any of this was written (see
 * CHANGES.md), models {@code StonecutterInventory}/{@code LoomInventory}/{@code
 * CartographyInventory} as plain, empty marker interfaces -- NOT one dedicated "prepare" event
 * each the way the crafting table ({@code PrepareItemCraftEvent}) and smithing table ({@code
 * PrepareSmithingEvent}) both get. That's why this listener works entirely off a plain {@link
 * InventoryClickEvent} type-check rather than a dedicated event, and why it has no BOTCHED/ruined
 * risk the way {@link CraftingMasteryListener}'s crafting-table/smithing-table rolls do: there's
 * no real "before it's taken" hook here to preview-and-rewrite the way {@code
 * CraftingInventory#setResult}/{@code SmithingInventory#setResult} allow, only the click itself,
 * so the only thing this listener can safely do is grow what's ALREADY sitting in the result slot
 * at the moment of the click -- never shrink or void it, since there's no safe way to undo a
 * real click already past the point of being cancelled cleanly on every server version this
 * project targets. A pure, no-risk bonus, by construction rather than by choice.
 *
 * <p>Deliberately does not gate specific loom {@code PatternType}s behind a mastery level --
 * doing that needs reading back which banner pattern is actually selected in a real {@code
 * LoomInventory}, a real accessor this stub tree's own audit found isn't declared on {@code
 * LoomInventory} at all (it's real, internal, client-driven GUI state with no real Bukkit-side
 * getter); see README's "known, deliberate design choices" section for the full reasoning. The
 * bonus-duplicate roll below needs no such accessor -- it only ever looks at the finished result
 * item already sitting in the slot.
 */
final class StationOverhaulListener implements Listener {

    private static final Set<InventoryAction> TAKE_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_ONE, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP, InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.COLLECT_TO_CURSOR, InventoryAction.SWAP_WITH_CURSOR);

    private final CraftingConfig config;
    private final CraftingSkillStore skillStore;
    private final Random random = new Random();

    StationOverhaulListener(CraftingConfig config, CraftingSkillStore skillStore) {
        this.config = config;
        this.skillStore = skillStore;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!config.enabled || event.getSlotType() != InventoryType.SlotType.RESULT
                || !TAKE_ACTIONS.contains(event.getAction())) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        String stationName = stationNameFor(clicked);
        if (stationName == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        int level = skillStore.levelOf(player, config);
        skillStore.addXp(player, config.xpPerStationUse);

        double chance = CraftingSkill.stationBonusChance(level, config);
        if (chance > 0.0 && random.nextDouble() < chance) {
            current.setAmount(current.getAmount() + 1);
            player.sendMessage("§a✦ A keen eye at the " + stationName + " -- you got an extra item.");
        }
    }

    private static String stationNameFor(Inventory inventory) {
        if (inventory instanceof StonecutterInventory) {
            return "stonecutter";
        }
        if (inventory instanceof LoomInventory) {
            return "loom";
        }
        if (inventory instanceof CartographyInventory) {
            return "cartography table";
        }
        return null;
    }
}
