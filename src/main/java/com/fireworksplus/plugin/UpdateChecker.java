package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class UpdateChecker {

    public record Result(String currentVersion, String latestVersion, boolean outdated) {}

    private final JavaPlugin plugin;

    private String latestVersion;
    private long lastCheckedMs;
    private boolean outdated;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("update_checker.enabled", false);
    }

    public void checkForUpdatesAsync(Consumer<Result> callback) {
        if (!isEnabled()) return;
        int resourceId = plugin.getConfig().getInt("update_checker.resource_id", 0);
        if (resourceId <= 0) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result result = fetchResult(resourceId);
            if (result != null) {
                latestVersion = result.latestVersion();
                outdated = result.outdated();
                lastCheckedMs = System.currentTimeMillis();
                if (callback != null) {
                    callback.accept(result);
                }
            }
        });
    }

    public void notifyIfOutdated(Player player) {
        if (!isEnabled() || player == null) return;
        if (!hasPermission(player)) return;

        if (!isCacheFresh()) {
            checkForUpdatesAsync(result -> {
                if (result.outdated()) {
                    sendUpdateMessage(player, result);
                }
            });
            return;
        }

        if (outdated && latestVersion != null) {
            sendUpdateMessage(player, new Result(currentVersion(), latestVersion, true));
        }
    }

    public void notifyOnlineAdminsIfOutdated() {
        if (!isEnabled()) return;
        if (!isCacheFresh()) {
            checkForUpdatesAsync(result -> {
                if (result.outdated()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (hasPermission(player)) {
                                sendUpdateMessage(player, result);
                            }
                        }
                    });
                }
            });
            return;
        }

        if (outdated && latestVersion != null) {
            Result result = new Result(currentVersion(), latestVersion, true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (hasPermission(player)) {
                    sendUpdateMessage(player, result);
                }
            }
        }
    }

    private boolean isCacheFresh() {
        int minutes = plugin.getConfig().getInt("update_checker.cache_minutes", 60);
        long maxAgeMs = Duration.ofMinutes(Math.max(1, minutes)).toMillis();
        return lastCheckedMs > 0 && (System.currentTimeMillis() - lastCheckedMs) <= maxAgeMs;
    }

    private Result fetchResult(int resourceId) {
        String current = currentVersion();
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=132276" + resourceId);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String latest = reader.readLine();
                if (latest == null || latest.isBlank()) return null;
                boolean isOutdated = compareVersions(current, latest) < 0;
                return new Result(current, latest.trim(), isOutdated);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private String currentVersion() {
        return plugin.getDescription().getVersion();
    }

    private int compareVersions(String current, String latest) {
        List<Integer> currentParts = versionParts(current);
        List<Integer> latestParts = versionParts(latest);
        int max = Math.max(currentParts.size(), latestParts.size());
        for (int i = 0; i < max; i++) {
            int a = i < currentParts.size() ? currentParts.get(i) : 0;
            int b = i < latestParts.size() ? latestParts.get(i) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        if (current.equalsIgnoreCase(latest)) return 0;
        return current.toLowerCase(Locale.ROOT).compareTo(latest.toLowerCase(Locale.ROOT));
    }

    private List<Integer> versionParts(String version) {
        List<Integer> parts = new ArrayList<>();
        if (version == null) return parts;
        String[] tokens = version.split("[^0-9]+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            try {
                parts.add(Integer.parseInt(token));
            } catch (NumberFormatException ignored) {
                parts.add(0);
            }
        }
        return parts;
    }

    private boolean hasPermission(Player player) {
        String node = plugin.getConfig().getString("update_checker.notify_permission", "fireworksplus.admin.update");
        return player.hasPermission("fireworksplus.*") || player.hasPermission(node);
    }

    private void sendUpdateMessage(Player player, Result result) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(ChatColor.GOLD + "[FireworksPlus] " + ChatColor.YELLOW + "A new version is available.");
            player.sendMessage(ChatColor.GRAY + "Current: " + ChatColor.WHITE + result.currentVersion()
                    + ChatColor.GRAY + " | Latest: " + ChatColor.WHITE + result.latestVersion());
        });
    }
}