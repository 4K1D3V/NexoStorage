package gg.kite.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {
    public static void playSoundAndParticles(Player player, Location location, String soundStr, String particleStr) {
        if (soundStr != null) {
            try {
                Sound sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
                player.playSound(location, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Invalid sound, skip
            }
        }
        if (particleStr != null) {
            try {
                String[] parts = particleStr.split(":");
                Particle particle = Particle.valueOf(parts[0].toUpperCase());
                int count = Integer.parseInt(parts[1]);
                double offsetX = Double.parseDouble(parts[2]);
                double offsetY = Double.parseDouble(parts[3]);
                double offsetZ = Double.parseDouble(parts[4]);
                player.spawnParticle(particle, location.add(0.5, 0.5, 0.5), count, offsetX, offsetY, offsetZ);
            } catch (Exception e) {
                // nothing
            }
        }
    }
}