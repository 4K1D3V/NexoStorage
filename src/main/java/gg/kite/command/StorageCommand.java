package gg.kite.command;

import com.nexomc.nexo.api.NexoBlocks;
import gg.kite.Main;
import gg.kite.config.ConfigManager;
import gg.kite.hooks.AdventureLib;
import gg.kite.hooks.NexoAPI;
import gg.kite.manager.StorageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StorageCommand implements CommandExecutor {
    private final Main plugin;
    private final StorageManager storageManager;
    private final ConfigManager configManager;

    public StorageCommand(Main plugin, StorageManager storageManager, NexoAPI nexoAPI, ConfigManager configManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            AdventureLib.sendMessage(sender, "This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            AdventureLib.sendMessage(sender, "Usage: /" + label + " <reload|listids|place|stats>");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "reload":
                if (!sender.hasPermission("nexostorage.reload")) {
                    AdventureLib.sendMessage(sender, "No permission to reload config!");
                    return true;
                }
                configManager.reloadConfig();
                AdventureLib.sendMessage(sender, "Config reloaded successfully!");
                if (configManager.isLoggingEnabled()) {
                    plugin.getLogger().info("Config reloaded by " + sender.getName());
                }
                return true;

            case "listids":
                if (!sender.hasPermission("nexostorage.admin")) {
                    AdventureLib.sendMessage(sender, "No permission to list block IDs!");
                    return true;
                }
                String ids = String.join(", ", NexoBlocks.blockIDs());
                AdventureLib.sendMessage(sender, "Nexo Block IDs: " + (ids.isEmpty() ? "None" : ids));
                return true;

            case "place":
                if (!sender.hasPermission("nexostorage.admin")) {
                    AdventureLib.sendMessage(sender, "No permission to place blocks!");
                    return true;
                }
                if (args.length < 2) {
                    AdventureLib.sendMessage(sender, "Usage: /" + label + " place <itemId> [x] [y] [z]");
                    return true;
                }
                String itemId = args[1];
                if (!configManager.getStorageBlockTypes().contains(itemId)) {
                    AdventureLib.sendMessage(sender, "Invalid or unconfigured block ID: " + itemId);
                    return true;
                }
                Location loc = args.length == 5 ? parseLocation(player, args[2], args[3], args[4]) : player.getLocation().getBlock().getLocation();
                if (loc == null) {
                    AdventureLib.sendMessage(sender, "Invalid coordinates!");
                    return true;
                }
                try {
                    NexoBlocks.place(itemId, loc);
                    AdventureLib.sendMessage(sender, "Placed '" + itemId + "' at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                    if (configManager.isLoggingEnabled()) {
                        plugin.getLogger().info("Placed " + itemId + " at " + loc + " by " + sender.getName());
                    }
                } catch (Exception e) {
                    AdventureLib.sendMessage(sender, "Failed to place block: " + e.getMessage());
                }
                return true;

            case "stats":
                if (!sender.hasPermission("nexostorage.admin")) {
                    AdventureLib.sendMessage(sender, "No permission to view stats!");
                    return true;
                }
                AdventureLib.sendMessage(sender, "Active Storage Blocks: " + storageManager.getActiveStorageCount());
                return true;

            default:
                AdventureLib.sendMessage(sender, "Unknown subcommand. Usage: /" + label + " <reload|listids|place|stats>");
                return true;
        }
    }

    private @Nullable Location parseLocation(@NotNull Player player, @NotNull String x, @NotNull String y, @NotNull String z) {
        try {
            int bx = x.startsWith("~") ? player.getLocation().getBlockX() + (x.length() > 1 ? Integer.parseInt(x.substring(1)) : 0) : Integer.parseInt(x);
            int by = y.startsWith("~") ? player.getLocation().getBlockY() + (y.length() > 1 ? Integer.parseInt(y.substring(1)) : 0) : Integer.parseInt(y);
            int bz = z.startsWith("~") ? player.getLocation().getBlockZ() + (z.length() > 1 ? Integer.parseInt(z.substring(1)) : 0) : Integer.parseInt(z);
            return new Location(player.getWorld(), bx, by, bz);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
