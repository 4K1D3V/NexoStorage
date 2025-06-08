package gg.kite.metrics;

import gg.kite.Main;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class PluginStats {
    private final Main plugin;
    private final int BSTATS_ID;

    public PluginStats(Main plugin, int BSTATS_ID) {
        this.plugin = plugin;
        this.BSTATS_ID = 26110;
    }

    public void Metrics(Main plugin, int BSTATS_ID) {
        Metrics metrics = new Metrics(plugin, BSTATS_ID);
        metrics.addCustomChart(new SimplePie("storage_blocks", () -> String.valueOf(plugin.getStorageManager().getActiveStorageCount())));
        plugin.getLogger().info("bStats metrics enabled. Disable in config.yml if desired.");
    }
}