package com.nexuscraft.nexuscrafting;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** {@code /nexuscrafting <skill|recipes|reload>} -- kept intentionally small, same "read-only
 *  status/admin surface, real gameplay happens through real vanilla interactions elsewhere"
 *  convention as NexusLode's own {@code LodeCommand}. */
final class CraftingCommand implements CommandExecutor {

    private final CraftingSkillStore skillStore;
    private final CraftingConfig config;
    private final SecretRecipeStore secretStore;
    private final Runnable reloadAction;

    CraftingCommand(CraftingSkillStore skillStore, CraftingConfig config, SecretRecipeStore secretStore,
                     Runnable reloadAction) {
        this.skillStore = skillStore;
        this.config = config;
        this.secretStore = secretStore;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7/nexuscrafting <skill|recipes|reload>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "skill" -> handleSkill(sender);
            case "recipes" -> handleRecipes(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§7/nexuscrafting <skill|recipes|reload>");
        }
        return true;
    }

    private void handleSkill(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player has crafting mastery to check.");
            return;
        }
        int level = skillStore.levelOf(player, config);
        long xp = skillStore.xpOf(player);
        long nextLevelXp = CraftingSkill.xpForNextLevel(level, config);
        String nextLine = nextLevelXp < 0
                ? "§7(max level)"
                : "§7" + xp + " / " + nextLevelXp + " XP to next level";
        player.sendMessage("§6Crafting mastery level " + level + " §7-- " + xp + " total XP");
        player.sendMessage(nextLine);
    }

    private void handleRecipes(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can check their own secret-recipe progress.");
            return;
        }
        int level = skillStore.levelOf(player, config);
        player.sendMessage("§6Secret recipes:");
        for (SecretRecipeDefinition definition : SecretRecipeCatalog.ALL) {
            boolean unlocked = secretStore.hasUnlocked(player, definition);
            if (unlocked) {
                player.sendMessage("§a✓ " + definition.id() + " §7-- " + definition.flavorText());
            } else {
                player.sendMessage("§7? " + definition.id() + " §8-- unlocks at crafting mastery level "
                        + definition.unlockLevel() + " (currently " + level + ")");
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexuscrafting.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return;
        }
        reloadAction.run();
        sender.sendMessage("§aNexusCrafting config reloaded.");
    }
}
