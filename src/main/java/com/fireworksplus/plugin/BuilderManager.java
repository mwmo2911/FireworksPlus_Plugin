package com.fireworksplus.plugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuilderManager {

    private final JavaPlugin plugin;
    private final Map<UUID, BuilderSession> sessions = new HashMap<>();

    public BuilderManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public BuilderSession getOrCreate(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), (id) -> {
            BuilderSession s = new BuilderSession();
            s.palette = plugin.getConfig().getStringList("shows.celebration.palette");
            if (s.palette == null) s.palette = java.util.List.of();
            return s;
        });
    }

    public BuilderSession get(Player p) {
        return sessions.get(p.getUniqueId());
    }

    public void clear(Player p) {
        sessions.remove(p.getUniqueId());
    }

    public BuilderSession startEditing(Player p, DraftShow show) {
        BuilderSession s = new BuilderSession();
        s.name = show.name;
        s.points.addAll(show.points);
        s.durationSeconds = show.durationSeconds;
        s.intervalTicks = show.intervalTicks;
        s.radius = show.radius;
        s.powerMin = show.powerMin;
        s.powerMax = show.powerMax;
        s.fireworkTypes = new java.util.ArrayList<>(show.fireworkTypes);
        s.palette = new java.util.ArrayList<>(show.palette);
        s.particleTrail = show.particleTrail;
        s.trailParticles = new java.util.ArrayList<>(show.trailParticles);
        s.collectingPoints = false;
        sessions.put(p.getUniqueId(), s);
        return s;
    }

}
