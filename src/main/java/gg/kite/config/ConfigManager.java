package gg.kite.config;

import com.nexomc.nexo.api.NexoItems;
import gg.kite.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final Main plugin;
    private final Map<String, StorageBlockConfig> storageBlocks = new HashMap<>();
    private String databaseType;
    private long saveInterval;
    private boolean antiDupe;
    private boolean logging;
    private boolean metrics;
    private int defaultInventorySize;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        storageBlocks.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("storage-blocks");
        if (section != null) {
            List<String> invalidIds = new ArrayList<>();
            for (String itemId : section.getKeys(false)) {
                if (!NexoItems.exists(itemId)) {
                    invalidIds.add(itemId);
                    continue;
                }
                ConfigurationSection blockSection = section.getConfigurationSection(itemId);
                storageBlocks.put(itemId, new StorageBlockConfig(blockSection));
            }
            if (!invalidIds.isEmpty()) {
                plugin.getLogger().warning("Invalid Nexo item IDs in config.yml: " + invalidIds);
            }
        }
        if (storageBlocks.isEmpty()) {
            plugin.getLogger().warning("No valid storage blocks defined in config.yml.");
        }

        databaseType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        saveInterval = plugin.getConfig().getLong("database.save-interval-seconds", 300);
        antiDupe = plugin.getConfig().getBoolean("anti-dupe", true);
        logging = plugin.getConfig().getBoolean("logging", true);
        metrics = plugin.getConfig().getBoolean("metrics", true);
        defaultInventorySize = Math.max(1, Math.min(6, plugin.getConfig().getInt("default-inventory-size", 5)));
    }

    public List<String> getStorageBlockTypes() {
        return new ArrayList<>(storageBlocks.keySet());
    }

    public StorageBlockConfig getStorageBlockConfig(String itemId) {
        return storageBlocks.getOrDefault(itemId, new StorageBlockConfig(null));
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public long getSaveInterval() {
        return saveInterval;
    }

    public boolean isAntiDupeEnabled() {
        return antiDupe;
    }

    public boolean isLoggingEnabled() {
        return logging;
    }

    public boolean isMetricsEnabled() {
        return metrics;
    }

    public int getDefaultInventorySize() {
        return defaultInventorySize;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
}