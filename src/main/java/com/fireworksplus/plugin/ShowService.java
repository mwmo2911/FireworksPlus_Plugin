package com.fireworksplus.plugin;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ShowService {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    private final Map<UUID, BukkitTask> activeShow = new HashMap<>();
    private final Map<UUID, Long> lastStartMs = new HashMap<>();

    public ShowService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String playShow(Player player, String showId) {
        String policy = checkPolicy(player);
        if (policy != null) return policy;

        Started started = playBuiltInAt(player.getLocation(), showId);
        if (started == null) return "Show not found.";

        markStarted(player, started.task());
        return null;
    }

    public boolean playBuiltInScheduled(Location at, String showId) {
        return playBuiltInAt(at, showId) != null;
    }

    public String playCustom(Player owner, DraftShow custom) {
        if (custom == null) return "Show not found.";
        if (custom.points.isEmpty()) return "Custom show has no points.";

        String policy = checkPolicy(owner);
        if (policy != null) return policy;

        BukkitTask task = runCustomPoints(custom, owner);
        markStarted(owner, task);
        return null;
    }

    public boolean playCustomScheduled(DraftShow custom) {
        if (custom == null || custom.points.isEmpty()) return false;
        runCustomPoints(custom, null);
        return true;
    }

    public boolean stopShow(Player player) {
        BukkitTask task = activeShow.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            return true;
        }
        return false;
    }

    private String checkPolicy(Player player) {
        int cooldownSec = plugin.getConfig().getInt("runtime.cooldown_seconds", 0);
        boolean allowSimul = plugin.getConfig().getBoolean("runtime.allow_simultaneous_shows", false);
        boolean cancelPrevious = plugin.getConfig().getBoolean("runtime.cancel_previous_show", true);

        if (cooldownSec > 0) {
            long now = System.currentTimeMillis();
            long last = lastStartMs.getOrDefault(player.getUniqueId(), 0L);
            long waitMs = cooldownSec * 1000L;

            if (now - last < waitMs) {
                long left = (waitMs - (now - last) + 999) / 1000;
                return "Please wait " + left + "s before starting another show.";
            }
        }

        BukkitTask existing = activeShow.get(player.getUniqueId());
        if (existing != null && !existing.isCancelled()) {
            if (!allowSimul) {
                if (cancelPrevious) {
                    existing.cancel();
                    activeShow.remove(player.getUniqueId());
                } else {
                    return "You already have a show running.";
                }
            }
        }

        return null;
    }

    private void markStarted(Player player, BukkitTask task) {
        activeShow.put(player.getUniqueId(), task);
        lastStartMs.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void cleanup(UUID uuid) {
        activeShow.remove(uuid);
    }

    private record Started(BukkitTask task) {}

    private Started playBuiltInAt(Location base, String showId) {
        if (base == null) return null;

        ConfigurationSection s = plugin.getConfig().getConfigurationSection("shows." + showId);
        if (s == null) return null;

        int fireworks = s.getInt("fireworks", 30);
        int interval = s.getInt("interval_ticks", 6);
        double radius = s.getDouble("radius", 1.6);
        int pMin = s.getInt("power_min", 1);
        int pMax = s.getInt("power_max", 2);
        List<String> palette = s.getStringList("palette");

        int minInterval = plugin.getConfig().getInt("limits.min_interval_ticks", 4);
        int maxTotal = plugin.getConfig().getInt("limits.max_fireworks_total", 250);

        interval = Math.max(interval, minInterval);
        fireworks = Math.min(Math.max(1, fireworks), maxTotal);

        World world = base.getWorld();
        if (world == null) return null;

        final int total = fireworks;
        final int finalInterval = interval;
        final Location fixedBase = base.clone();

        playSoundSafe(fixedBase, plugin.getConfig().getString("sounds.start", ""));

        BukkitRunnable runnable = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= total) {
                    playSoundSafe(fixedBase, plugin.getConfig().getString("sounds.end", ""));
                    cancel();
                    return;
                }

                Location loc = fixedBase.clone().add(rand(-radius, radius), 0.2, rand(-radius, radius));
                Firework fw = world.spawn(loc, Firework.class);
                if (plugin.getConfig().getBoolean("sounds.mute_vanilla_fireworks", false)) {
                    fw.setSilent(true);
                }

                FireworkMeta meta = fw.getFireworkMeta();
                meta.clearEffects();
                meta.addEffect(randomEffectFromPalette(palette));
                meta.setPower(randInt(pMin, pMax));
                fw.setFireworkMeta(meta);
                Particle builtInParticle = pickParticle(s.getStringList("particles"));
                if (builtInParticle != null) {
                    attachParticleTrail(fw, builtInParticle);
                }

                playSoundSafe(loc, plugin.getConfig().getString("sounds.each_firework", ""));

                i++;
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 0L, finalInterval);
        return new Started(task);
    }

    private BukkitTask runCustomPoints(DraftShow show, Player ownerOrNull) {
        int minInterval = plugin.getConfig().getInt("limits.min_interval_ticks", 4);
        int maxTotal = plugin.getConfig().getInt("limits.max_fireworks_total", 250);

        int interval = Math.max(show.intervalTicks, minInterval);

        int total = Math.max(1, (show.durationSeconds * 20) / interval);
        total = Math.min(total, maxTotal);

        final int finalTotal = total;
        final int finalInterval = interval;

        List<Location> points = new ArrayList<>();
        for (Location l : show.points) points.add(l.clone());

        final Location first = points.get(0);

        playSoundSafe(first, plugin.getConfig().getString("sounds.start", ""));

        BukkitRunnable runnable = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (ownerOrNull != null && !ownerOrNull.isOnline()) {
                    cleanup(ownerOrNull.getUniqueId());
                    cancel();
                    return;
                }

                if (i >= finalTotal) {
                    playSoundSafe(first, plugin.getConfig().getString("sounds.end", ""));
                    if (ownerOrNull != null) cleanup(ownerOrNull.getUniqueId());
                    cancel();
                    return;
                }

                Location base = points.get(i % points.size());
                World world = base.getWorld();
                if (world == null) {
                    i++;
                    return;
                }

                Location loc = base.clone().add(rand(-show.radius, show.radius), 0.2, rand(-show.radius, show.radius));
                Firework fw = world.spawn(loc, Firework.class);
                if (plugin.getConfig().getBoolean("sounds.mute_vanilla_fireworks", false)) {
                    fw.setSilent(true);
                }

                FireworkMeta meta = fw.getFireworkMeta();
                meta.clearEffects();
                meta.addEffect(effectFromPalette(show.palette, show.fireworkTypes));
                meta.setPower(randInt(show.powerMin, show.powerMax));
                fw.setFireworkMeta(meta);
                Particle trail = pickTrailParticle(show);
                if (trail != null) {
                    attachParticleTrail(fw, trail);
                }

                playSoundSafe(loc, plugin.getConfig().getString("sounds.each_firework", ""));

                i++;
            }
        };

        return runnable.runTaskTimer(plugin, 0L, finalInterval);
    }

    private FireworkEffect randomEffectFromPalette(List<String> palette) {
        return effectFromPalette(palette, null);
    }

    private FireworkEffect effectFromPalette(List<String> palette, List<String> typeNames) {
        FireworkEffect.Type type = pickType(typeNames);
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

    private FireworkEffect.Type pickType(List<String> typeNames) {
        if (typeNames != null && !typeNames.isEmpty()) {
            String raw = typeNames.get(random.nextInt(typeNames.size()));
            if (raw != null) {
                try {
                    return FireworkEffect.Type.valueOf(raw.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        return types[random.nextInt(types.length)];
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

    private void playSoundSafe(Location at, String soundName) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) return;
        if (soundName == null || soundName.isBlank()) return;
        if (at == null || at.getWorld() == null) return;

        float vol = (float) plugin.getConfig().getDouble("sounds.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.pitch", 1.0);

        Sound s;
        try {
            s = Sound.valueOf(soundName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return;
        }

        at.getWorld().playSound(at, s, vol, pitch);
    }

    private void attachParticleTrail(Firework firework, Particle particle) {
        if (firework == null || particle == null) return;
        Vector initialVelocity = firework.getVelocity();
        if (initialVelocity.lengthSquared() < 0.01) {
            firework.setVelocity(new Vector(0, 0.5, 0));
        }
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!firework.isValid() || firework.isDead()) {
                    cancel();
                    return;
                }

                Location loc = firework.getLocation().add(0, 0.25, 0);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(particle, loc, 2, 0.05, 0.05, 0.05, 0.0);
                }

                ticks++;
                if (ticks > 80) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Particle pickTrailParticle(DraftShow show) {
        if (show == null) return null;
        if (show.trailParticles != null && !show.trailParticles.isEmpty()) {
            return pickParticle(show.trailParticles);
        }
        if (show.particleTrail) {
            return trailParticle();
        }
        return null;
    }

    private Particle trailParticle() {
        String configured = plugin.getConfig().getString("particles.trail_type", "FIREWORKS_SPARK");
        if (configured == null || configured.isBlank()) return null;
        try {
            return Particle.valueOf(configured.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Particle pickParticle(List<String> names) {
        if (names == null || names.isEmpty()) return null;
        List<String> valid = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                Particle.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
                valid.add(name.trim());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
        }
        if (valid.isEmpty()) return null;
        String selected = valid.get(random.nextInt(valid.size()));
        try {
            return Particle.valueOf(selected.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
