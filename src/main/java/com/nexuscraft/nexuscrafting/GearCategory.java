package com.nexuscraft.nexuscrafting;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Classifies a crafted item's real {@code Material} into the kind of real, equippable gear it is
 * -- entirely this project's own invented grouping (real Bukkit has no such enum), used only to
 * decide which real {@code Attribute}/{@code EquipmentSlotGroup} pair {@link
 * CraftQualityItemTagger} bakes a quality bonus onto. Classified by real Material name suffix
 * rather than an exhaustive, hand-maintained list -- the same real naming convention every real
 * vanilla tool/weapon/armor Material already follows (every sword really does end in
 * {@code _SWORD}, every pickaxe in {@code _PICKAXE}, and so on), so this stays correct even
 * against this sandbox's own necessarily-trimmed {@code Material} enum without needing a matching
 * per-material table kept in sync by hand.
 *
 * <p>{@link #TOOL} deliberately covers axes alongside pickaxes/shovels/hoes under one real
 * attribute ({@code Attribute.MINING_EFFICIENCY}, which real vanilla applies to any correct-tool
 * block break, wood included) rather than splitting "mining tool" from "woodcutting tool" --
 * there's no second real tool-speed attribute to split it onto. Durability/repair is
 * deliberately untouched here (and everywhere else in this plugin) -- that's NexusForge's own
 * already-shipped, already-owned mechanic; see README's "known boundaries" section.
 */
enum GearCategory {
    WEAPON(Attribute.ATTACK_DAMAGE, EquipmentSlotGroup.HAND),
    TOOL(Attribute.MINING_EFFICIENCY, EquipmentSlotGroup.HAND),
    ARMOR(Attribute.ARMOR, EquipmentSlotGroup.ARMOR),
    NONE(null, null);

    private final Attribute attribute;
    private final EquipmentSlotGroup slotGroup;

    GearCategory(Attribute attribute, EquipmentSlotGroup slotGroup) {
        this.attribute = attribute;
        this.slotGroup = slotGroup;
    }

    /** {@code null} only for {@link #NONE}. */
    Attribute attribute() {
        return attribute;
    }

    /** {@code null} only for {@link #NONE}. */
    EquipmentSlotGroup slotGroup() {
        return slotGroup;
    }

    boolean isEquippable() {
        return this != NONE;
    }

    static GearCategory of(Material material) {
        if (material == null) {
            return NONE;
        }
        String name = material.name();
        if (name.endsWith("_SWORD") || name.equals("TRIDENT") || name.equals("BOW") || name.equals("CROSSBOW")) {
            return WEAPON;
        }
        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {
            return TOOL;
        }
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || name.equals("SHIELD")) {
            return ARMOR;
        }
        return NONE;
    }
}
