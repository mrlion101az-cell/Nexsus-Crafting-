package com.nexuscraft.nexuscrafting;

/**
 * The math behind a player's crafting mastery -- levels up from real, resolved hand-crafts (a
 * workbench {@code CraftItemEvent} taken, a smithing-table {@code SmithItemEvent} taken, or a
 * bonus roll at the stonecutter/loom/cartography table), and feeds three real outcome rolls
 * {@link CraftingMasteryListener} applies to the item sitting in a crafting menu's result slot
 * before it's taken: a chance of a CRITICAL craft (one bonus item), a chance of a BOTCHED craft
 * (the result comes out empty -- ingredients spent for nothing), and a chance of a cost refund
 * (one random, already-placed ingredient keeps one extra unit instead of being consumed). XP
 * required for level {@code n} follows {@code xpBase * n^1.5}, rounded -- the same fixed curve
 * shape NexusLode's own {@code MiningSkill} (and NexusCuisine's {@code CookingSkill} before it)
 * already uses, reused deliberately rather than inventing a third shape this family would then
 * have to maintain. Total XP is what's actually persisted ({@link CraftingSkillStore}); level is
 * always derived from it here rather than stored redundantly, so the two can never drift apart.
 *
 * <p>Deliberately does NOT model a "craft speed" bonus -- real vanilla has no attribute or
 * server-side delay governing how fast a hand-craft at a crafting table resolves (every real
 * click is instant), so there was nothing real for a speed bonus to attach to. Bonus yield
 * (critical craft), ingredient savings (cost refund), and the quality-tier ceiling below take its
 * place instead -- see README's "known, deliberate design choices" section.
 */
final class CraftingSkill {

    private CraftingSkill() {
    }

    static long xpForLevel(int level, CraftingConfig config) {
        return Math.round(config.skillXpBase * Math.pow(level, 1.5));
    }

    static int levelFor(long totalXp, CraftingConfig config) {
        int level = 1;
        while (level < config.skillMaxLevel && totalXp >= xpForLevel(level + 1, config)) {
            level++;
        }
        return level;
    }

    static long xpIntoCurrentLevel(long totalXp, int level, CraftingConfig config) {
        return totalXp - xpForLevel(level, config);
    }

    static long xpForNextLevel(int level, CraftingConfig config) {
        return level >= config.skillMaxLevel ? -1 : xpForLevel(level + 1, config);
    }

    /** Real odds (0.0-1.0, clamped) of a CRITICAL craft -- an extra bonus item -- at this level.
     *  Zero growth below level 1 (there is none); grows linearly above it, same shape as every
     *  other per-level bonus this family already uses. */
    static double criticalChance(int level, CraftingConfig config) {
        return clamp01(config.baseCriticalChance + Math.max(0, level - 1) * config.criticalChancePerLevel);
    }

    /** Real odds of a BOTCHED craft -- the result comes out empty, ingredients spent for nothing
     *  -- highest for a brand-new crafter, shrinking toward (but, by design, never quite reaching)
     *  zero as mastery grows. A server operator who wants this risk gone entirely can set both
     *  {@code base-botch-chance} and {@code botch-chance-reduction-per-level} to {@code 0} in
     *  config.yml. */
    static double botchChance(int level, CraftingConfig config) {
        return clamp01(config.baseBotchChance - Math.max(0, level - 1) * config.botchChanceReductionPerLevel);
    }

    /** Real odds one random, non-empty ingredient slot in the crafting matrix keeps one extra
     *  unit instead of being consumed by this craft. Zero by default at level 1 -- this is a pure
     *  bonus mastery earns, never a risk. */
    static double costRefundChance(int level, CraftingConfig config) {
        return clamp01(config.baseCostRefundChance + Math.max(0, level - 1) * config.costRefundChancePerLevel);
    }

    /** Real odds of a bonus-yield roll succeeding at the stonecutter/loom/cartography table --
     *  same per-level growth shape as {@link #criticalChance}, kept as a separate, independently
     *  tunable number since those three stations have no "botch" risk counterpart at all (see
     *  {@code StationOverhaulListener}'s own comment for why). */
    static double stationBonusChance(int level, CraftingConfig config) {
        return clamp01(config.baseStationBonusChance + Math.max(0, level - 1) * config.stationBonusChancePerLevel);
    }

    static boolean qualityTierUnlocked(CraftQuality tier, int level, CraftingConfig config) {
        return switch (tier) {
            case STANDARD -> true;
            case FINE -> level >= config.qualityUnlockLevelFine;
            case SUPERIOR -> level >= config.qualityUnlockLevelSuperior;
            case MASTERWORK -> level >= config.qualityUnlockLevelMasterwork;
        };
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
