package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowStorage {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public ShowStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shows.yml");
        reload();
    }

    /** Deletes a custom show by id or name (normalized). Returns true if it existed and was removed. */
    public boolean deleteCustomShow(String idOrName) {
        String id = normalizeId(idOrName);
        String base = "custom." + id;

        if (id.isBlank()) return false;
        if (!yaml.contains(base)) return false;

        yaml.set(base, null);

        // If "custom" section becomes empty, remove it as well (clean file)
        ConfigurationSection sec = yaml.getConfigurationSection("custom");
        if (sec != null && sec.getKeys(false).isEmpty()) {
            yaml.set("custom", null);
        }

        saveFile();
        return true;
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void saveFile() {
        try { yaml.save(file); } catch (IOException ignored) {}
    }

    public void saveCustomShow(DraftShow d, Player creator) {
        String id = normalizeId(d.name);
        String base = "custom." + id;

        yaml.set(base + ".createdByName", creator.getName());
        yaml.set(base + ".createdByUuid", creator.getUniqueId().toString());
        yaml.set(base + ".createdAt", java.time.Instant.now().toString());

        yaml.set(base + ".durationSeconds", d.durationSeconds);
        yaml.set(base + ".intervalTicks", d.intervalTicks);
        yaml.set(base + ".radius", d.radius);
        yaml.set(base + ".powerMin", d.powerMin);
        yaml.set(base + ".powerMax", d.powerMax);
        yaml.set(base + ".effectTypes", d.fireworkTypes);
        yaml.set(base + ".palette", d.palette);

        List<Map<String, Object>> pts = new ArrayList<>();
        for (Location loc : d.points) {
            if (loc.getWorld() == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("world", loc.getWorld().getName());
            m.put("x", loc.getX());
            m.put("y", loc.getY());
            m.put("z", loc.getZ());
            pts.add(m);
        }
        yaml.set(base + ".points", pts);

        saveFile();
    }

    public DraftShow loadCustomShow(String idOrName) {
        String id = normalizeId(idOrName);
        String base = "custom." + id;

        if (!yaml.contains(base)) return null;

        DraftShow d = new DraftShow(id);
        d.durationSeconds = yaml.getInt(base + ".durationSeconds", 30);
        d.intervalTicks = yaml.getInt(base + ".intervalTicks", 6);
        d.radius = yaml.getDouble(base + ".radius", 1.6);
        d.powerMin = yaml.getInt(base + ".powerMin", 1);
        d.powerMax = yaml.getInt(base + ".powerMax", 2);
        List<String> types = yaml.getStringList(base + ".effectTypes");
        if (types == null || types.isEmpty()) {
            String legacy = yaml.getString(base + ".effectType", org.bukkit.FireworkEffect.Type.BALL.name());
            types = new ArrayList<>(List.of(legacy));
        }
        d.fireworkTypes = new ArrayList<>(types);
        d.palette = yaml.getStringList(base + ".palette");

        List<?> list = yaml.getList(base + ".points", List.of());
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;

            String wName = String.valueOf(m.get("world"));
            World w = Bukkit.getWorld(wName);
            if (w == null) continue;

            double x = Double.parseDouble(String.valueOf(m.get("x")));
            double y = Double.parseDouble(String.valueOf(m.get("y")));
            double z = Double.parseDouble(String.valueOf(m.get("z")));
            d.points.add(new Location(w, x, y, z));
        }

        return d;
    }

    public List<String> listCustomShows() {
        ConfigurationSection sec = yaml.getConfigurationSection("custom");
        if (sec == null) return List.of();
        return new ArrayList<>(sec.getKeys(false));
    }

    public boolean hasCustom(String idOrName) {
        String id = normalizeId(idOrName);
        return yaml.contains("custom." + id);
    }

    public static String normalizeId(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    public YamlConfiguration getYaml() {
        return yaml;
    }
}
