package com.nexuscraft.nexuscrafting;

import org.bukkit.Material;

import java.util.Random;

/**
 * Standalone pure-logic test suite -- a plain {@code main} method, no JUnit, no Bukkit runtime.
 * Exercises every class in this plugin that has no real Bukkit-server dependency at all: the
 * crafting-mastery XP/level curve, the critical/botch/cost-refund/station-bonus chance curves,
 * the quality-tier unlock gating and weighted roll, gear classification, and the secret-recipe
 * catalog's own lookups. Same convention as every other sibling in this family.
 */
public final class NexusCraftingLogicTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testSkillCurve();
        testCriticalChanceGrowth();
        testBotchChanceDecay();
        testCostRefundChanceGrowth();
        testStationBonusChanceGrowth();
        testQualityTierUnlockGating();
        testQualityRollerLevelFraction();
        testQualityRollerAtLevelOneAlwaysStandard();
        testQualityRollerAtMaxLevelReachesEveryTier();
        testCraftQualityConfigBonuses();
        testGearCategoryClassification();
        testSecretRecipeCatalogLookups();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSkillCurve() {
        CraftingConfig config = new CraftingConfig(null);

        expect(CraftingSkill.levelFor(0, config) == 1, "level 1 at zero XP");
        long level5Xp = CraftingSkill.xpForLevel(5, config);
        expect(CraftingSkill.levelFor(level5Xp, config) == 5, "exact level-5 XP lands on level 5");
        expect(CraftingSkill.levelFor(level5Xp - 1, config) == 4, "one XP short of level 5 stays level 4");

        int maxLevel = config.skillMaxLevel;
        long hugeXp = CraftingSkill.xpForLevel(maxLevel, config) + 1_000_000L;
        expect(CraftingSkill.levelFor(hugeXp, config) == maxLevel, "level never exceeds configured max");
        expect(CraftingSkill.xpForNextLevel(maxLevel, config) == -1, "no next level once at max");

        long into = CraftingSkill.xpIntoCurrentLevel(level5Xp + 7, 5, config);
        expect(into == 7, "xpIntoCurrentLevel measures progress past the level floor");
    }

    private static void testCriticalChanceGrowth() {
        CraftingConfig config = new CraftingConfig(null);
        double at1 = CraftingSkill.criticalChance(1, config);
        double at25 = CraftingSkill.criticalChance(25, config);
        double atMax = CraftingSkill.criticalChance(config.skillMaxLevel, config);
        expect(at1 == config.baseCriticalChance, "critical chance at level 1 is exactly the configured base");
        expect(at25 > at1, "critical chance grows with level");
        expect(atMax > at25, "critical chance keeps growing toward max level");
        expect(atMax <= 1.0, "critical chance never exceeds 100%");
    }

    private static void testBotchChanceDecay() {
        CraftingConfig config = new CraftingConfig(null);
        double at1 = CraftingSkill.botchChance(1, config);
        double at25 = CraftingSkill.botchChance(25, config);
        double atMax = CraftingSkill.botchChance(config.skillMaxLevel, config);
        expect(at1 == config.baseBotchChance, "botch chance at level 1 is exactly the configured base");
        expect(at25 < at1, "botch chance shrinks with level");
        expect(atMax < at25, "botch chance keeps shrinking toward max level");
        expect(atMax >= 0.0, "botch chance never goes negative");

        CraftingConfig disabled = new CraftingConfig(null);
        disabled.baseBotchChance = 0.0;
        disabled.botchChanceReductionPerLevel = 0.0;
        expect(CraftingSkill.botchChance(1, disabled) == 0.0, "a server can configure the botch risk away entirely");
    }

    private static void testCostRefundChanceGrowth() {
        CraftingConfig config = new CraftingConfig(null);
        expect(CraftingSkill.costRefundChance(1, config) == config.baseCostRefundChance,
                "cost refund chance at level 1 is exactly the configured base (zero by default)");
        double atMax = CraftingSkill.costRefundChance(config.skillMaxLevel, config);
        expect(atMax > config.baseCostRefundChance, "cost refund chance grows with level");
    }

    private static void testStationBonusChanceGrowth() {
        CraftingConfig config = new CraftingConfig(null);
        double at1 = CraftingSkill.stationBonusChance(1, config);
        double atMax = CraftingSkill.stationBonusChance(config.skillMaxLevel, config);
        expect(at1 == config.baseStationBonusChance, "station bonus chance at level 1 is exactly the configured base");
        expect(atMax > at1, "station bonus chance grows with level");
    }

    private static void testQualityTierUnlockGating() {
        CraftingConfig config = new CraftingConfig(null);

        expect(CraftingSkill.qualityTierUnlocked(CraftQuality.STANDARD, 1, config), "standard is always unlocked");
        expect(!CraftingSkill.qualityTierUnlocked(CraftQuality.FINE, config.qualityUnlockLevelFine - 1, config),
                "fine is locked just below its configured level");
        expect(CraftingSkill.qualityTierUnlocked(CraftQuality.FINE, config.qualityUnlockLevelFine, config),
                "fine unlocks exactly at its configured level");
        expect(!CraftingSkill.qualityTierUnlocked(CraftQuality.MASTERWORK, config.qualityUnlockLevelMasterwork - 1, config),
                "masterwork is locked just below its configured level");
        expect(CraftingSkill.qualityTierUnlocked(CraftQuality.MASTERWORK, config.qualityUnlockLevelMasterwork, config),
                "masterwork unlocks exactly at its configured level");
    }

    private static void testQualityRollerLevelFraction() {
        CraftingConfig config = new CraftingConfig(null);
        expect(CraftQualityRoller.levelFraction(1, config) == 0.0, "level-fraction is zero at level 1");
        expect(CraftQualityRoller.levelFraction(config.skillMaxLevel, config) == 1.0,
                "level-fraction reaches exactly 1.0 at max level");
        double mid = CraftQualityRoller.levelFraction(config.skillMaxLevel / 2, config);
        expect(mid > 0.0 && mid < 1.0, "level-fraction is strictly between 0 and 1 mid-progression");
    }

    private static void testQualityRollerAtLevelOneAlwaysStandard() {
        CraftingConfig config = new CraftingConfig(null);
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            expect(CraftQualityRoller.roll(1, config, random) == CraftQuality.STANDARD,
                    "a level-1 crafter always rolls STANDARD (every higher tier is locked)");
        }
    }

    private static void testQualityRollerAtMaxLevelReachesEveryTier() {
        CraftingConfig config = new CraftingConfig(null);
        Random random = new Random(7);
        boolean sawFine = false;
        boolean sawSuperior = false;
        boolean sawMasterwork = false;
        boolean sawStandard = false;
        for (int i = 0; i < 5000; i++) {
            switch (CraftQualityRoller.roll(config.skillMaxLevel, config, random)) {
                case FINE -> sawFine = true;
                case SUPERIOR -> sawSuperior = true;
                case MASTERWORK -> sawMasterwork = true;
                case STANDARD -> sawStandard = true;
            }
        }
        expect(sawFine, "a max-level crafter eventually rolls FINE over enough attempts");
        expect(sawSuperior, "a max-level crafter eventually rolls SUPERIOR over enough attempts");
        expect(sawMasterwork, "a max-level crafter eventually rolls MASTERWORK over enough attempts");
        expect(sawStandard, "even a max-level crafter still rolls STANDARD sometimes");
    }

    private static void testCraftQualityConfigBonuses() {
        CraftingConfig config = new CraftingConfig(null);
        expect(CraftQuality.STANDARD.attributeBonus(config) == 0.0, "standard quality carries no attribute bonus");
        expect(CraftQuality.FINE.attributeBonus(config) == config.qualityAttributeBonusFine,
                "fine quality's bonus is read from config");
        expect(CraftQuality.MASTERWORK.attributeBonus(config) > CraftQuality.SUPERIOR.attributeBonus(config),
                "masterwork's bonus is strictly bigger than superior's");
        expect(CraftQuality.STANDARD.loreLine() == null, "standard quality has no lore line at all");
        expect(CraftQuality.MASTERWORK.loreLine() != null, "masterwork quality has a lore line");
    }

    private static void testGearCategoryClassification() {
        expect(GearCategory.of(Material.DIAMOND_SWORD) == GearCategory.WEAPON, "a sword is a weapon");
        expect(GearCategory.of(Material.BOW) == GearCategory.WEAPON, "a bow is a weapon");
        expect(GearCategory.of(Material.DIAMOND_PICKAXE) == GearCategory.TOOL, "a pickaxe is a tool");
        expect(GearCategory.of(Material.IRON_AXE) == GearCategory.TOOL, "an axe is a tool");
        expect(GearCategory.of(Material.DIAMOND_HELMET) == GearCategory.ARMOR, "a helmet is armor");
        expect(GearCategory.of(Material.SHIELD) == GearCategory.ARMOR, "a shield counts as armor here");
        expect(GearCategory.of(Material.OAK_PLANKS) == GearCategory.NONE, "a plain block is not gear");
        expect(GearCategory.of(null) == GearCategory.NONE, "no material classifies as NONE rather than throwing");
        expect(!GearCategory.NONE.isEquippable(), "NONE is never equippable");
        expect(GearCategory.WEAPON.isEquippable(), "WEAPON is equippable");
    }

    private static void testSecretRecipeCatalogLookups() {
        expect(!SecretRecipeCatalog.ALL.isEmpty(), "the secret-recipe catalog is not empty");
        for (SecretRecipeDefinition definition : SecretRecipeCatalog.ALL) {
            expect(SecretRecipeCatalog.byId(definition.id()) == definition,
                    "byId finds the same definition for its own id: " + definition.id());
            expect(definition.unlockLevel() > 1, "every secret recipe requires more than the starting level: "
                    + definition.id());
        }
        expect(SecretRecipeCatalog.byId("not-a-real-recipe-id") == null,
                "an unknown id returns null rather than throwing");
    }

    private static void expect(boolean condition, String description) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + description);
        } else {
            failed++;
            System.out.println("FAIL: " + description);
        }
    }
}
