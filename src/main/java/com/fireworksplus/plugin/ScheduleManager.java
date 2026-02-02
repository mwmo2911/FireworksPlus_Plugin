package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScheduleManager {

    private final JavaPlugin plugin;
    private final ShowService shows;
    private final ShowStorage storage;

    private final File file;
    private YamlConfiguration yaml;

    private DateTimeFormatter fmt;

    public ScheduleManager(JavaPlugin plugin, ShowService shows, ShowStorage storage) {
        this.plugin = plugin;
        this.shows = shows;
        this.storage = storage;

        this.file = new File(plugin.getDataFolder(), "schedules.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        String pattern = plugin.getConfig().getString("scheduling.datetime_format", "yyyy-MM-dd HH:mm");
        fmt = DateTimeFormatter.ofPattern(pattern);
    }

    public boolean removeSchedule(String id) {
        if (id == null || id.isBlank()) return false;

        String base = "jobs." + id;
        if (!yaml.contains(base)) return false;

        yaml.set(base, null);
        saveFile();
        return true;
    }

    public void saveFile() {
        try { yaml.save(file); } catch (IOException ignored) {}
    }

    public String addSchedule(String showId, Location at, String date, String time) {
        if (at == null || at.getWorld() == null) return "Invalid location.";

        LocalDateTime when;
        try {
            when = LocalDateTime.parse(date + " " + time, fmt);
        } catch (Exception e) {
            return "Invalid datetime. Expected: yyyy-MM-dd HH:mm";
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        String base = "jobs." + id;

        yaml.set(base + ".showId", showId);
        yaml.set(base + ".when", when.format(fmt));
        yaml.set(base + ".done", false);

        yaml.set(base + ".world", at.getWorld().getName());
        yaml.set(base + ".x", at.getX());
        yaml.set(base + ".y", at.getY());
        yaml.set(base + ".z", at.getZ());

        saveFile();
        return null;
    }

    public void startPolling() {
        if (!plugin.getConfig().getBoolean("scheduling.enabled", true)) return;

        int poll = plugin.getConfig().getInt("scheduling.poll_seconds", 20);
        poll = Math.max(5, poll);

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, poll * 20L);
    }

    private void tick() {
        ConfigurationSection sec = yaml.getConfigurationSection("jobs");
        if (sec == null) return;

        LocalDateTime now = LocalDateTime.now();

        for (String id : sec.getKeys(false)) {
            String base = "jobs." + id;

            if (yaml.getBoolean(base + ".done", false)) continue;

            String whenText = yaml.getString(base + ".when", null);
            String showId = yaml.getString(base + ".showId", null);
            if (whenText == null || showId == null) continue;

            LocalDateTime when;
            try {
                when = LocalDateTime.parse(whenText, fmt);
            } catch (Exception ignored) {
                continue;
            }

            if (now.isBefore(when)) continue;

            String worldName = yaml.getString(base + ".world", null);
            World w = worldName == null ? null : Bukkit.getWorld(worldName);
            if (w == null) {
                yaml.set(base + ".done", true);
                saveFile();
                continue;
            }

            double x = yaml.getDouble(base + ".x");
            double y = yaml.getDouble(base + ".y");
            double z = yaml.getDouble(base + ".z");
            Location at = new Location(w, x, y, z);

            boolean ok;
            if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
                ok = shows.playBuiltInScheduled(at, showId);
            } else {
                DraftShow custom = storage.loadCustomShow(showId);
                ok = shows.playCustomScheduled(custom);
            }

            yaml.set(base + ".done", true);
            saveFile();

            if (!ok) {
                plugin.getLogger().warning("Scheduled job " + id + " failed: show not found " + showId);
            }
        }
    }

    public List<String> listSchedulesPretty() {
        ConfigurationSection sec = yaml.getConfigurationSection("jobs");
        if (sec == null) return List.of();

        List<String> out = new ArrayList<>();
        for (String id : sec.getKeys(false)) {
            String base = "jobs." + id;

            String showId = yaml.getString(base + ".showId", "?");
            String when = yaml.getString(base + ".when", "?");
            boolean done = yaml.getBoolean(base + ".done", false);

            String world = yaml.getString(base + ".world", "?");
            int x = (int) Math.round(yaml.getDouble(base + ".x"));
            int y = (int) Math.round(yaml.getDouble(base + ".y"));
            int z = (int) Math.round(yaml.getDouble(base + ".z"));

            String status = done ? (ChatColor.RED + "[DONE]") : (ChatColor.GREEN + "[PENDING]");

            out.add(status
                    + ChatColor.DARK_GRAY + " " + id
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + when
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + showId
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + world + " " + x + " " + y + " " + z);
        }
        return out;
    }

    public List<String> listSchedulesForShow(String showId) {
        ConfigurationSection sec = yaml.getConfigurationSection("jobs");
        if (sec == null) return List.of();

        List<String> out = new ArrayList<>();
        for (String id : sec.getKeys(false)) {
            String base = "jobs." + id;

            String sId = yaml.getString(base + ".showId", "");
            if (!sId.equalsIgnoreCase(showId)) continue;

            String when = yaml.getString(base + ".when", "?");
            boolean done = yaml.getBoolean(base + ".done", false);

            String world = yaml.getString(base + ".world", "?");
            int x = (int) Math.round(yaml.getDouble(base + ".x"));
            int y = (int) Math.round(yaml.getDouble(base + ".y"));
            int z = (int) Math.round(yaml.getDouble(base + ".z"));

            String status = done ? (ChatColor.RED + "[DONE]") : (ChatColor.GREEN + "[PENDING]");

            out.add(status
                    + ChatColor.DARK_GRAY + " " + id
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + when
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GRAY + world + " " + x + " " + y + " " + z);
        }

        return out;
    }
}
