package com.nexuscraft.nexuscrafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/** Parses config.yml's tunables. Same "config drives shared numbers, the catalog/class data holds
 *  the fixed game-balance shape" split this whole family already uses (compare NexusLode's own
 *  {@code LodeConfig}) -- {@link SecretRecipeCatalog}'s own recipe definitions and {@link
 *  CraftQuality}'s own tier ladder are the fixed half of that split; everything below is the
 *  tunable half. */
final class CraftingConfig {

    private final Plugin plugin;

    boolean enabled = true;

    // -- Crafting mastery skill (CraftingSkill/CraftingSkillStore) --
    int skillMaxLevel = 50;
    double skillXpBase = 6.0;
    long xpPerCraft = 4;
    long xpPerSmith = 10;
    long xpPerStationUse = 3;

    // -- Critical/botched crafting (CraftingSkill#criticalChance/botchChance) --
    double baseCriticalChance = 0.02;
    double criticalChancePerLevel = 0.004;
    double baseBotchChance = 0.12;
    double botchChanceReductionPerLevel = 0.0022;

    // -- Ingredient-saving ("cost refund") roll (CraftingSkill#costRefundChance) --
    double baseCostRefundChance = 0.0;
    double costRefundChancePerLevel = 0.003;

    // -- Quality tiers, gear only (CraftQuality/CraftQualityRoller) --
    int qualityUnlockLevelFine = 5;
    int qualityUnlockLevelSuperior = 20;
    int qualityUnlockLevelMasterwork = 40;
    /** Flat, additive {@code AttributeModifier.Operation.ADD_SCALAR} bonus baked onto a tier's
     *  crafted gear -- see {@link CraftQualityItemTagger}. */
    double qualityAttributeBonusFine = 0.03;
    double qualityAttributeBonusSuperior = 0.07;
    double qualityAttributeBonusMasterwork = 0.14;

    // -- Specialized station overhaul (StationOverhaulListener) --
    double baseStationBonusChance = 0.01;
    double stationBonusChancePerLevel = 0.003;

    CraftingConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(FileConfiguration c) {
        Logger log = plugin != null ? plugin.getLogger() : null;

        enabled = c.getBoolean("enabled", enabled);

        ConfigurationSection skill = c.getConfigurationSection("skill");
        if (skill != null) {
            skillMaxLevel = Math.max(1, skill.getInt("max-level", skillMaxLevel));
            skillXpBase = Math.max(1.0, skill.getDouble("xp-base", skillXpBase));
            xpPerCraft = Math.max(0, skill.getInt("xp-per-craft", (int) xpPerCraft));
            xpPerSmith = Math.max(0, skill.getInt("xp-per-smith", (int) xpPerSmith));
            xpPerStationUse = Math.max(0, skill.getInt("xp-per-station-use", (int) xpPerStationUse));
        }

        ConfigurationSection outcome = c.getConfigurationSection("outcome-rolls");
        if (outcome != null) {
            baseCriticalChance = clamp01(outcome.getDouble("base-critical-chance", baseCriticalChance));
            criticalChancePerLevel = clampNonNegative(outcome.getDouble("critical-chance-per-level", criticalChancePerLevel));
            baseBotchChance = clamp01(outcome.getDouble("base-botch-chance", baseBotchChance));
            botchChanceReductionPerLevel = clampNonNegative(outcome.getDouble("botch-chance-reduction-per-level", botchChanceReductionPerLevel));
            baseCostRefundChance = clamp01(outcome.getDouble("base-cost-refund-chance", baseCostRefundChance));
            costRefundChancePerLevel = clampNonNegative(outcome.getDouble("cost-refund-chance-per-level", costRefundChancePerLevel));
        }

        ConfigurationSection quality = c.getConfigurationSection("quality");
        if (quality != null) {
            qualityUnlockLevelFine = Math.max(1, quality.getInt("unlock-level-fine", qualityUnlockLevelFine));
            qualityUnlockLevelSuperior = Math.max(qualityUnlockLevelFine, quality.getInt("unlock-level-superior", qualityUnlockLevelSuperior));
            qualityUnlockLevelMasterwork = Math.max(qualityUnlockLevelSuperior, quality.getInt("unlock-level-masterwork", qualityUnlockLevelMasterwork));
            qualityAttributeBonusFine = clampNonNegative(quality.getDouble("attribute-bonus-fine", qualityAttributeBonusFine));
            qualityAttributeBonusSuperior = clampNonNegative(quality.getDouble("attribute-bonus-superior", qualityAttributeBonusSuperior));
            qualityAttributeBonusMasterwork = clampNonNegative(quality.getDouble("attribute-bonus-masterwork", qualityAttributeBonusMasterwork));
        }

        ConfigurationSection stations = c.getConfigurationSection("stations");
        if (stations != null) {
            baseStationBonusChance = clamp01(stations.getDouble("base-bonus-chance", baseStationBonusChance));
            stationBonusChancePerLevel = clampNonNegative(stations.getDouble("bonus-chance-per-level", stationBonusChancePerLevel));
        }

        if (!enabled && log != null) {
            log.info("NexusCrafting is disabled via config.yml.");
        }
    }

    private static double clampNonNegative(double value) {
        return Math.max(0.0, value);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
