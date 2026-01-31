package com.fireworksplus.plugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DraftManager {

    private final JavaPlugin plugin;
    private final Map<UUID, DraftShow> drafts = new HashMap<>();

    public DraftManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public DraftShow create(Player p, String name) {
        DraftShow d = new DraftShow(name);
        d.palette = plugin.getConfig().getStringList("shows.celebration.palette");
        drafts.put(p.getUniqueId(), d);
        return d;
    }

    public DraftShow get(Player p) {
        return drafts.get(p.getUniqueId());
    }

    public void clear(Player p) {
        drafts.remove(p.getUniqueId());
    }
}
