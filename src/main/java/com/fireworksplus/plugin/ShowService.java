package com.fireworksplus.plugin;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class ShowService {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ShowService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean playShow(Player player, String showId) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("shows." + showId);
        if (s == null) return false;

        int fireworks = s.getInt("fireworks", 30);
        int interval = s.getInt("interval_ticks", 6);
        double radius = s.getDouble("radius", 1.6);
        int pMin = s.getInt("power_min", 1);
        int pMax = s.getInt("power_max", 2);
        List<String> palette = s.getStringList("palette");

        // safety limits
        int minInterval = plugin.getConfig().getInt("limits.min_interval_ticks", 4);
        int maxTotal = plugin.getConfig().getInt("limits.max_fireworks_total", 250);

        interval = Math.max(interval, minInterval);
        fireworks = Math.min(Math.max(1, fireworks), maxTotal);

        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) return true;

        final int total = fireworks;
        final int finalInterval = interval;

        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (i >= total) { cancel(); return; }

                Location loc = base.clone().add(rand(-radius, radius), 0.2, rand(-radius, radius));
                Firework fw = world.spawn(loc, Firework.class);

                FireworkMeta meta = fw.getFireworkMeta();
                meta.clearEffects();
                meta.addEffect(randomEffectFromPalette(palette));
                meta.setPower(randInt(pMin, pMax));
                fw.setFireworkMeta(meta);

                i++;
            }
        }.runTaskTimer(plugin, 0L, finalInterval);

        return true;
    }

    private FireworkEffect randomEffectFromPalette(List<String> palette) {
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type type = types[random.nextInt(types.length)];

        Color main = paletteColor(palette);
        Color fade = paletteColor(palette);

        return FireworkEffect.builder()
                .with(type)
                .withColor(main)
                .withFade(fade)
                .trail(random.nextBoolean())
                .flicker(random.nextBoolean())
                .build();
    }

    private Color paletteColor(List<String> palette) {
        if (palette == null || palette.isEmpty()) {
            return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        }

        String hex = palette.get(random.nextInt(palette.size())).replace("#", "").trim();
        try {
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int b = rgb & 255;
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return Color.fromRGB(255, 0, 0);
        }
    }

    private double rand(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private int randInt(int min, int max) {
        if (max < min) return min;
        return min + random.nextInt(max - min + 1);
    }
}
