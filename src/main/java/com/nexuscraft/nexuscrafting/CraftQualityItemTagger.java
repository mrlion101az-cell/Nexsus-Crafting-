package com.nexuscraft.nexuscrafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bakes a rolled {@link CraftQuality} onto a crafted {@code ItemStack} -- a PDC marker (so a
 * later {@code /nexuscrafting inspect}-style lookup or another listener can read the tier back
 * off the item itself), a flavor lore line, a quality-prefixed display name, and -- only for gear
 * {@link GearCategory} classifies as equippable -- a real, flat {@code AttributeModifier} bonus
 * scaled by {@link CraftQuality#attributeBonus}.
 */
final class CraftQualityItemTagger {

    private final NamespacedKey qualityKey;
    private final NamespacedKey modifierKey;

    CraftQualityItemTagger(Plugin plugin) {
        this.qualityKey = new NamespacedKey(plugin, "quality");
        this.modifierKey = new NamespacedKey(plugin, "quality_bonus");
    }

    /** Tags {@code item} with {@code quality}, in place. A deliberate no-op for {@link
     *  CraftQuality#STANDARD} -- an ordinary craft is left exactly as real vanilla would have made
     *  it: no tag, no lore, no renamed display, no bonus. Only a craft that rolled above standard
     *  gets touched at all, so a server with quality tiers disabled (every tier locked via
     *  config) never sees a single crafted item change shape. */
    void tag(ItemStack item, CraftQuality quality, CraftingConfig config) {
        if (item == null || quality == CraftQuality.STANDARD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, quality.name());

        String baseName = meta.getDisplayName();
        if (baseName == null) {
            baseName = prettyName(item.getType());
        }
        meta.setDisplayName(quality.displayPrefix() + baseName);

        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
        String line = quality.loreLine();
        if (line != null) {
            lore.add(line);
        }
        meta.setLore(lore);

        GearCategory category = GearCategory.of(item.getType());
        if (category.isEquippable()) {
            double bonus = quality.attributeBonus(config);
            if (bonus > 0.0) {
                AttributeModifier modifier = new AttributeModifier(
                        modifierKey, bonus, AttributeModifier.Operation.ADD_SCALAR, category.slotGroup());
                meta.addAttributeModifier(category.attribute(), modifier);
            }
        }

        item.setItemMeta(meta);
    }

    /** Reads the quality tag back off an item -- {@link CraftQuality#STANDARD} for an untagged
     *  item, matching {@link #tag}'s own no-op convention (a standard craft was never tagged at
     *  all, so "no tag" and "standard" mean the same thing here). */
    CraftQuality qualityOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return CraftQuality.STANDARD;
        }
        String stored = item.getItemMeta().getPersistentDataContainer().get(qualityKey, PersistentDataType.STRING);
        if (stored == null) {
            return CraftQuality.STANDARD;
        }
        try {
            return CraftQuality.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return CraftQuality.STANDARD;
        }
    }

    private static String prettyName(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
