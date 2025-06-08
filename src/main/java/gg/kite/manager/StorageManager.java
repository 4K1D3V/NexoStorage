package gg.kite.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.kite.Main;
import gg.kite.config.ConfigManager;
import gg.kite.config.StorageBlockConfig;
import gg.kite.hooks.NexoAPI;
import gg.kite.util.ItemUtil;
import gg.kite.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class StorageManager {
    private final Main plugin;
    private final ConfigManager configManager;
    private final NexoAPI nexoAPI;
    private final ConcurrentHashMap<Location, Inventory> storageInventories = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Location> dirtyInventories = new ConcurrentLinkedQueue<>();
    private HikariDataSource dataSource;
    private boolean useDatabase;

    public StorageManager(Main plugin, @NotNull ConfigManager configManager, NexoAPI nexoAPI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.nexoAPI = nexoAPI;
        this.useDatabase = "sqlite".equalsIgnoreCase(configManager.getDatabaseType());
        if (useDatabase) {
            initDatabase();
        }
        loadStorages();
    }

    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "storage.db");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            dataSource = new HikariDataSource(config);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS storages (location TEXT PRIMARY KEY, items BLOB, transaction_id TEXT)"
                 )) {
                stmt.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database, falling back to YAML", e);
            useDatabase = false;
            dataSource = null;
        }
    }

    public Inventory getStorageInventory(Location location, String itemId) {
        return storageInventories.computeIfAbsent(location, loc -> {
            StorageBlockConfig blockConfig = configManager.getStorageBlockConfig(itemId);
            PersistentDataContainer dataContainer = new CraftPersistentDataContainer(new HashMap<>());
            StorageHolder holder = new StorageHolder(loc, dataContainer);
            Inventory inv = Bukkit.createInventory(holder, blockConfig.getSize(), blockConfig.getTitle());
            if (configManager.isAntiDupeEnabled()) {
                ItemUtil.setTransactionId(inv, UUID.randomUUID().toString());
            }
            loadInventoryFromStorage(loc, inv, itemId);
            return inv;
        });
    }

    private void loadInventoryFromStorage(Location location, Inventory inv, String itemId) {
        if (useDatabase) {
            loadFromDatabase(location, inv);
        } else {
            loadFromYaml(location, inv);
        }
    }

    public void removeStorage(Location location) {
        Inventory inv = storageInventories.remove(location);
        if (inv != null && configManager.isAntiDupeEnabled()) {
            ItemUtil.invalidateTransactionId(inv);
        }
        dirtyInventories.remove(location);
        if (useDatabase) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM storages WHERE location = ?")) {
                stmt.setString(1, LocationUtil.locationToString(location));
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to remove storage from database", e);
            }
        }
    }

    public void markInventoryForSave(Location location) {
        dirtyInventories.add(location);
    }

    public void saveStorages() {
        if (dirtyInventories.isEmpty()) return;
        if (useDatabase) {
            saveToDatabase();
        } else {
            saveToYaml();
        }
    }

    private void saveToDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO storages (location, items, transaction_id) VALUES (?, ?, ?)"
            )) {
                while (!dirtyInventories.isEmpty()) {
                    Location loc = dirtyInventories.poll();
                    Inventory inv = storageInventories.get(loc);
                    if (inv == null) continue;
                    stmt.setString(1, LocationUtil.locationToString(loc));
                    stmt.setBytes(2, ItemUtil.serializeItems(inv.getContents()));
                    stmt.setString(3, configManager.isAntiDupeEnabled() ? ItemUtil.getTransactionId(inv) : null);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Failed to save storage data to database", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
        }
    }

    private void saveToYaml() {
        File storageFile = new File(plugin.getDataFolder(), "storage.yml");
        YamlConfiguration config = new YamlConfiguration();
        while (!dirtyInventories.isEmpty()) {
            Location loc = dirtyInventories.poll();
            Inventory inv = storageInventories.get(loc);
            if (inv == null) continue;
            String key = LocationUtil.locationToString(loc);
            config.set("storages." + key + ".items", inv.getContents());
            if (configManager.isAntiDupeEnabled()) {
                config.set("storages." + key + ".transaction_id", ItemUtil.getTransactionId(inv));
            }
        }
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save storage data to YAML", e);
        }
    }

    private void loadFromDatabase(Location location, Inventory inv) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT items, transaction_id FROM storages WHERE location = ?")) {
            stmt.setString(1, LocationUtil.locationToString(location));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ItemStack[] items = ItemUtil.deserializeItems(rs.getBytes("items"));
                    inv.setContents(items);
                    if (configManager.isAntiDupeEnabled()) {
                        String transactionId = rs.getString("transaction_id");
                        if (transactionId != null) {
                            ItemUtil.setTransactionId(inv, transactionId);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load storage at " + location, e);
        }
    }

    private void loadFromYaml(Location location, Inventory inv) {
        File storageFile = new File(plugin.getDataFolder(), "storage.yml");
        if (!storageFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        String key = LocationUtil.locationToString(location);
        if (!config.contains("storages." + key)) return;
        Object itemsObj = config.get("storages." + key + ".items");
        if (itemsObj instanceof ItemStack[] items) {
            inv.setContents(items);
            if (configManager.isAntiDupeEnabled()) {
                String transactionId = config.getString("storages." + key + ".transaction_id");
                if (transactionId != null) {
                    ItemUtil.setTransactionId(inv, transactionId);
                }
            }
        }
    }

    private void loadStorages() {
        if (useDatabase) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT location FROM storages");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Location loc = LocationUtil.stringToLocation(rs.getString("location"));
                    String itemId = nexoAPI.getNexoBlockType(loc.getBlock());
                    if (itemId != null && configManager.getStorageBlockTypes().contains(itemId)) {
                        getStorageInventory(loc, itemId);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load storage locations", e);
            }
        } else {
            File storageFile = new File(plugin.getDataFolder(), "storage.yml");
            if (!storageFile.exists()) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
            if (!config.contains("storages")) return;
            for (String key : Objects.requireNonNull(config.getConfigurationSection("storages")).getKeys(false)) {
                try {
                    Location loc = LocationUtil.stringToLocation(key);
                    String itemId = nexoAPI.getNexoBlockType(loc.getBlock());
                    if (itemId != null && configManager.getStorageBlockTypes().contains(itemId)) {
                        getStorageInventory(loc, itemId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load storage at " + key + ": " + e.getMessage());
                }
            }
        }
    }

    public void closeDatabase() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public int getActiveStorageCount() {
        return storageInventories.size();
    }
}