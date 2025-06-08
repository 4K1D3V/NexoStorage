package gg.kite;

import gg.kite.command.StorageCommand;
import gg.kite.command.StorageTab;
import gg.kite.config.ConfigManager;
import gg.kite.hooks.NexoAPI;
import gg.kite.listener.StorageListener;
import gg.kite.manager.StorageManager;
import gg.kite.metrics.PluginStats;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Main extends JavaPlugin {
    private ConfigManager configManager;
    private StorageManager storageManager;
    private NexoAPI nexoAPI;
    private int BSTATS_ID;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        nexoAPI = new NexoAPI(this);
        storageManager = new StorageManager(this, configManager, nexoAPI);

        getServer().getPluginManager().registerEvents(new StorageListener(this, storageManager, nexoAPI, configManager), this);

        StorageCommand command = new StorageCommand(this, storageManager, nexoAPI, configManager);
        Objects.requireNonNull(getCommand("nexostorage")).setExecutor(command);
        Objects.requireNonNull(getCommand("nexostorage")).setTabCompleter(new StorageTab());

        long saveIntervalTicks = configManager.getSaveInterval() * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, storageManager::saveStorages, saveIntervalTicks, saveIntervalTicks);

        new PluginStats(this, BSTATS_ID);

        checkNexoVersion();
        getLogger().info("NexoStorage v1.0 enabled successfully!");
    }

    @Override
    public void onDisable() {
        storageManager.saveStorages();
        storageManager.closeDatabase();
        getLogger().info("NexoStorage disabled and data saved.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public NexoAPI getNexoAPI() {
        return nexoAPI;
    }

    private void checkNexoVersion() {
        String nexoVersion = Objects.requireNonNull(getServer().getPluginManager().getPlugin("Nexo")).getDescription().getVersion();
        if (!"1.7.1".equals(nexoVersion)) {
            getLogger().warning("Nexo version " + nexoVersion + " detected. This plugin is tested with Nexo 1.7.1. Compatibility issues may occur.");
        }
    }
}