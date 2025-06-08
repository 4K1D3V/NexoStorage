package gg.kite.config;

import org.bukkit.configuration.ConfigurationSection;

public class StorageBlockConfig {
    private final String title;
    private final int size;
    private final String sound;
    private final String particles;
    private final boolean requireLock;
    private final String permission;

    public StorageBlockConfig(ConfigurationSection section) {
        if (section == null) {
            title = "Nexo Storage";
            size = 5;
            sound = null;
            particles = null;
            requireLock = false;
            permission = null;
        } else {
            title = section.getString("title", "Nexo Storage");
            size = Math.max(1, Math.min(6, section.getInt("size", 5))) * 9;
            sound = section.getString("sound");
            particles = section.getString("particles");
            requireLock = section.getBoolean("require-lock", false);
            permission = section.getString("permission");
        }
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public String getSound() {
        return sound;
    }

    public String getParticles() {
        return particles;
    }

    public boolean isLockRequired() {
        return requireLock;
    }

    public String getPermission() {
        return permission;
    }
}