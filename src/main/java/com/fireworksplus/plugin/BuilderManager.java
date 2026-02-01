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
            // default palette from a built-in show if it exists
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
}
