package gg.kite.api;

import gg.kite.Main;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class NexoStorageAPI {
    private final Main plugin;

    public NexoStorageAPI(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isStorageBlock(Block block) {
        String itemId = plugin.getNexoAPI().getNexoBlockType(block);
        return itemId != null && plugin.getConfigManager().getStorageBlockTypes().contains(itemId);
    }

    public Inventory getStorageInventory(Location location, String itemId) {
        return plugin.getStorageManager().getStorageInventory(location, itemId);
    }

    public boolean openStorage(Player player, @NotNull Location location) {
        String itemId = plugin.getNexoAPI().getNexoBlockType(location.getBlock());
        if (itemId == null || !plugin.getConfigManager().getStorageBlockTypes().contains(itemId)) {
            return false;
        }
        if (plugin.getNexoAPI().canPlayerAccess(player, location.getBlock())) {
            return false;
        }
        Inventory inv = getStorageInventory(location, itemId);
        player.openInventory(inv);
        return true;
    }
}
