package com.nexuscraft.nexuscrafting;

import java.util.Random;

/**
 * Rolls a {@link CraftQuality} tier for a gear craft -- a weighted random pick across whichever
 * tiers {@link CraftingSkill#qualityTierUnlocked} currently allows, same "chance, not a straight
 * threshold" reasoning as NexusCuisine's own {@code CookingSkill#rollQuality}. Weight for each
 * unlocked tier above {@link CraftQuality#STANDARD} grows linearly with how far through the
 * configured level range the crafter is (0 at level 1, a fixed cap at {@code skillMaxLevel}) --
 * the three non-standard weights are scaled so they can never claim more than {@link
 * #MAX_NON_STANDARD_WEIGHT} of the total between them, so STANDARD always keeps at least that
 * much weight of its own: a level-1 crafter (nothing else unlocked) always gets STANDARD, and
 * even a max-level master still lands on STANDARD a meaningful fraction of the time rather than
 * Masterwork-or-better every single craft.
 */
final class CraftQualityRoller {

    /** The three non-standard tiers can never claim more than this fraction of the total roll
     *  weight, however high the fraction climbs -- the rest always stays STANDARD's. */
    private static final double MAX_NON_STANDARD_WEIGHT = 0.7;

    private CraftQualityRoller() {
    }

    static CraftQuality roll(int level, CraftingConfig config, Random random) {
        double fraction = levelFraction(level, config);

        double masterworkShare = CraftingSkill.qualityTierUnlocked(CraftQuality.MASTERWORK, level, config) ? 0.25 : 0.0;
        double superiorShare = CraftingSkill.qualityTierUnlocked(CraftQuality.SUPERIOR, level, config) ? 0.35 : 0.0;
        double fineShare = CraftingSkill.qualityTierUnlocked(CraftQuality.FINE, level, config) ? 0.55 : 0.0;
        double shareTotal = masterworkShare + superiorShare + fineShare;

        double nonStandardWeight = MAX_NON_STANDARD_WEIGHT * fraction;
        double masterworkWeight = shareTotal > 0.0 ? nonStandardWeight * (masterworkShare / shareTotal) : 0.0;
        double superiorWeight = shareTotal > 0.0 ? nonStandardWeight * (superiorShare / shareTotal) : 0.0;
        double fineWeight = shareTotal > 0.0 ? nonStandardWeight * (fineShare / shareTotal) : 0.0;
        double standardWeight = Math.max(0.0, 1.0 - masterworkWeight - superiorWeight - fineWeight);

        double total = standardWeight + fineWeight + superiorWeight + masterworkWeight;
        double roll = random.nextDouble() * total;

        if (roll < masterworkWeight) {
            return CraftQuality.MASTERWORK;
        }
        roll -= masterworkWeight;
        if (roll < superiorWeight) {
            return CraftQuality.SUPERIOR;
        }
        roll -= superiorWeight;
        if (roll < fineWeight) {
            return CraftQuality.FINE;
        }
        return CraftQuality.STANDARD;
    }

    /** 0.0 at level 1, 1.0 at {@code skillMaxLevel}, linear in between. Exposed package-private
     *  so the test suite can check the weighting curve directly without reaching into {@link
     *  #roll}'s own randomness. */
    static double levelFraction(int level, CraftingConfig config) {
        int span = Math.max(1, config.skillMaxLevel - 1);
        return Math.min(1.0, Math.max(0.0, (level - 1) / (double) span));
    }
}
