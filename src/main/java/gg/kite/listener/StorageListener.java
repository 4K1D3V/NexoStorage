package gg.kite.listener;

import gg.kite.Main;
import gg.kite.config.ConfigManager;
import gg.kite.config.StorageBlockConfig;
import gg.kite.hooks.AdventureLib;
import gg.kite.hooks.NexoAPI;
import gg.kite.listener.event.StorageAccessEvent;
import gg.kite.listener.event.StorageCloseEvent;
import gg.kite.listener.event.StorageOpenEvent;
import gg.kite.manager.StorageHolder;
import gg.kite.manager.StorageManager;
import gg.kite.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StorageListener implements Listener {
    private final Main plugin;
    private final StorageManager storageManager;
    private final NexoAPI nexoAPI;
    private final ConfigManager configManager;

    public StorageListener(Main plugin, StorageManager storageManager, NexoAPI nexoAPI, ConfigManager configManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.nexoAPI = nexoAPI;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || nexoAPI.isNexoBlock(block)) return;

        String itemId = nexoAPI.getNexoBlockType(block);
        if (itemId == null || !configManager.getStorageBlockTypes().contains(itemId)) return;

        StorageBlockConfig blockConfig = configManager.getStorageBlockConfig(itemId);
        if (blockConfig.isLockRequired() && nexoAPI.canPlayerAccess(event.getPlayer(), block)) {
            AdventureLib.sendMessage(event.getPlayer(), "You don't have permission or lock access to this storage!");
            return;
        }

        StorageAccessEvent accessEvent = new StorageAccessEvent(event.getPlayer(), block.getLocation());
        Bukkit.getPluginManager().callEvent(accessEvent);

        StorageOpenEvent openEvent = new StorageOpenEvent(event.getPlayer(), block.getLocation());
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) return;

        Inventory inv = storageManager.getStorageInventory(block.getLocation(), itemId);
        event.getPlayer().openInventory(inv);
        SoundUtil.playSoundAndParticles(event.getPlayer(), block.getLocation(), blockConfig.getSound(), blockConfig.getParticles());
        if (configManager.isLoggingEnabled()) {
            plugin.getLogger().info("Player " + event.getPlayer().getName() + " opened storage at " + block.getLocation());
        }
    }

    @EventHandler
    public void onBlockDropItem(@NotNull BlockDropItemEvent event) {
        handleBlockBreak(event.getBlock(), event.getItems());
    }

    /*
    @EventHandler
    public void onNexoBlockBreak(@NotNull NexoBlockBreakEvent event) {
        handleBlockBreak(event.getBlock(), event.getDroppedItems());
    }
    */

    private void handleBlockBreak(Block block, List<Item> droppedItems) {
        if (nexoAPI.isNexoBlock(block)) return;

        String itemId = nexoAPI.getNexoBlockType(block);
        if (itemId == null || !configManager.getStorageBlockTypes().contains(itemId)) return;

        Location loc = block.getLocation();
        Inventory inv = storageManager.getStorageInventory(loc, itemId);
        if (inv != null) {
            for (ItemStack item : inv.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    droppedItems.add(block.getWorld().dropItemNaturally(loc, item.clone()));
                }
            }
            storageManager.removeStorage(loc);
            if (configManager.isLoggingEnabled()) {
                plugin.getLogger().info("Storage at " + loc + " broken, items dropped.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageHolder holder)) return;
        Bukkit.getPluginManager().callEvent(new StorageCloseEvent((Player) event.getPlayer(), holder.getLocation()));
        storageManager.markInventoryForSave(holder.getLocation());
    }
}