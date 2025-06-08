package gg.kite.hooks;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import gg.kite.Main;
import gg.kite.config.StorageBlockConfig;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public class NexoAPI {
    private final Main plugin;

    public NexoAPI(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isNexoBlock(@Nullable Block block) {
        return block == null || !NexoBlocks.isCustomBlock(block);
    }

    @Nullable
    public String getNexoBlockType(@Nullable Block block) {
        if (isNexoBlock(block)) return null;
        assert block != null;
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
        if (mechanic == null) return null;

        ItemStack item = null;
        try {
            item = (ItemStack) mechanic.getClass().getMethod("getItem").invoke(mechanic);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {

        }

        String itemId = item != null ? NexoItems.idFromItem(item) : null;
        if (itemId != null && NexoItems.exists(itemId)) {
            return itemId;
        }

        try {
            itemId = (String) mechanic.getClass().getMethod("getItemID").invoke(mechanic);
            if (NexoItems.exists(itemId)) {
                return itemId;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        plugin.getLogger().warning("Could not retrieve item ID for Nexo block at " + block.getLocation());
        return null;
    }

    public boolean canPlayerAccess(@Nullable Player player, @Nullable Block block) {
        if (player == null || isNexoBlock(block)) return true;
        assert block != null;
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
        if (mechanic == null) return true;

        try {
            Boolean canAccess = (Boolean) mechanic.getClass().getMethod("canAccess", Player.class, Block.class)
                    .invoke(mechanic, player, block);
            if (canAccess != null && !canAccess) {
                return true;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Could not check lock access for Nexo block at " + block.getLocation() +
                    ". Ensure CustomBlockMechanic has a canAccess(Player, Block) method.");
        }

        String itemId = getNexoBlockType(block);
        if (itemId == null) return true;
        StorageBlockConfig config = plugin.getConfigManager().getStorageBlockConfig(itemId);
        String permission = config.getPermission();
        return permission != null && !player.hasPermission(permission) && !player.hasPermission("nexostorage.open.*");
    }
}