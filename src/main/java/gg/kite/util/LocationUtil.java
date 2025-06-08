package gg.kite.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class LocationUtil {
    public static @NotNull String locationToString(@NotNull Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }

    @Contract("_ -> new")
    public static @NotNull Location stringToLocation(@NotNull String str) {
        String[] parts = str.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid location string format: " + str);
        }
        World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + parts[0]);
        }
        try {
            return new Location(
                    world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinates in location string: " + str);
        }
    }
}
