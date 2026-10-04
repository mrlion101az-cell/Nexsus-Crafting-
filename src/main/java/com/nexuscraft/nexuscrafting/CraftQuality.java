package com.nexuscraft.nexuscrafting;

/**
 * A hand-crafted item's real, baked-in quality tier -- entirely this project's own invented
 * ladder, same "real vanilla has no concept of this at all" reasoning as this family's own
 * {@code DishQuality} (NexusCuisine), whose exact weighted-random-pick-rather-than-threshold
 * styling {@link CraftQualityRoller} mirrors. Unlike a cooked dish, a tier here is only ever
 * applied to gear {@link GearCategory} actually classifies as equippable (a weapon, tool, or
 * armor piece) -- there is no real attribute to bake a bonus onto for a crafted block or a stack
 * of arrows, so {@link #STANDARD} is both "an ordinary craft" AND "every non-gear craft," and
 * {@link CraftQualityItemTagger} is a deliberate no-op for it.
 */
enum CraftQuality {
    STANDARD,
    FINE,
    SUPERIOR,
    MASTERWORK;

    /** Prefixed onto the item's real display name -- a plain, un-prefixed name for {@link
     *  #STANDARD} (an ordinary craft looks exactly like vanilla's own, no cosmetic tell at all). */
    String displayPrefix() {
        return switch (this) {
            case STANDARD -> "";
            case FINE -> "§aFine ";
            case SUPERIOR -> "§bSuperior ";
            case MASTERWORK -> "§6§lMasterwork ";
        };
    }

    /** {@code null} for {@link #STANDARD} -- see {@link CraftQualityItemTagger#tag}'s own
     *  comment for why a standard craft gets no lore line at all, not even an empty one. */
    String loreLine() {
        return switch (this) {
            case STANDARD -> null;
            case FINE -> "§7A cut above an ordinary craft.";
            case SUPERIOR -> "§7Genuinely impressive work.";
            case MASTERWORK -> "§7The kind of craft a smith signs their name to.";
        };
    }

    /** Flat {@code AttributeModifier.Operation.ADD_SCALAR} bonus this tier bakes onto eligible
     *  gear -- the tunable half of this ladder, read from {@link CraftingConfig} rather than
     *  fixed here, same "config drives shared numbers" split as every other per-tier bonus this
     *  family uses. Always {@code 0.0} for {@link #STANDARD}. */
    double attributeBonus(CraftingConfig config) {
        return switch (this) {
            case STANDARD -> 0.0;
            case FINE -> config.qualityAttributeBonusFine;
            case SUPERIOR -> config.qualityAttributeBonusSuperior;
            case MASTERWORK -> config.qualityAttributeBonusMasterwork;
        };
    }
}
