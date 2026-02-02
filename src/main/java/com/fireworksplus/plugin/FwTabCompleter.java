package com.fireworksplus.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FwTabCompleter implements TabCompleter {

    private final FireworksPlus plugin;
    private final ShowStorage storage;
    private final ScheduleManager schedule;

    public FwTabCompleter(FireworksPlus plugin, ShowStorage storage, ScheduleManager schedule) {
        this.plugin = plugin;
        this.storage = storage;
        this.schedule = schedule;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("help");
            subs.add("play");
            subs.add("list");
            subs.add("info");
            subs.add("stop");
            subs.add("create");
            subs.add("addpoint");
            subs.add("duration");
            subs.add("save");
            subs.add("version");

            if (hasPermission(player, "fireworksplus.builder")) {
                subs.add("edit");
            }

            if (hasPermission(player, "fireworksplus.admin.schedule")) {
                subs.add("schedule");
                subs.add("schedules");
                subs.add("unschedule");
            }

            if (hasPermission(player, "fireworksplus.admin.reload")) {
                subs.add("reload");
            }

            return filterPrefix(subs, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if ((sub.equals("play") || sub.equals("info") || sub.equals("schedule")) && args.length == 2) {
            return filterPrefix(allShows(), args[1]);
        }

        if (sub.equals("edit") && args.length == 2) {
            return filterPrefix(customShows(), args[1]);
        }

        if (sub.equals("unschedule") && args.length == 2) {
            return filterPrefix(scheduleIds(), args[1]);
        }

        if (sub.equals("schedule") && args.length == 3) {
            return List.of("yyyy-MM-dd");
        }
        if (sub.equals("schedule") && args.length == 4) {
            return List.of("HH:mm");
        }

        return List.of();
    }

    private List<String> allShows() {
        List<String> out = new ArrayList<>();

        var sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            out.addAll(sec.getKeys(false));
        }

        out.addAll(storage.listCustomShows());

        return out.stream().distinct().collect(Collectors.toList());
    }

    private List<String> customShows() {
        return new ArrayList<>(storage.listCustomShows());
    }

    private List<String> scheduleIds() {
        List<String> lines = schedule.listSchedulesPretty();
        List<String> ids = new ArrayList<>();
        for (String line : lines) {
            String plain = org.bukkit.ChatColor.stripColor(line);
            String[] parts = plain.split("\\s+");
            if (parts.length >= 2) ids.add(parts[1]);
        }
        return ids;
    }

    private List<String> filterPrefix(List<String> items, String prefixRaw) {
        String prefix = prefixRaw.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean hasPermission(Player player, String node) {
        return player.hasPermission("fireworksplus.*") || player.hasPermission(node);
    }
}
