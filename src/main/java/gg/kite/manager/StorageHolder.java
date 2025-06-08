package gg.kite.manager;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;

public class StorageHolder implements InventoryHolder, PersistentDataHolder {
    private final Location location;
    private final PersistentDataContainer dataContainer;

    public StorageHolder(@NotNull Location location, @NotNull PersistentDataContainer dataContainer) {
        this.location = location;
        this.dataContainer = dataContainer;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null; // Managed by StorageManager
    }

    public @NotNull Location getLocation() {
        return location;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return dataContainer;
    }
}