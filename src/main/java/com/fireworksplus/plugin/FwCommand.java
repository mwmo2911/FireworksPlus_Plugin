package com.fireworksplus.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class FwCommand implements CommandExecutor {

    private final FireworksPlus plugin;
    private final ShowMenu menu;
    private final ShowService shows;
    private final ShowStorage storage;
    private final DraftManager drafts;
    private final ScheduleManager schedule;

    public FwCommand(FireworksPlus plugin, ShowMenu menu, ShowService shows, ShowStorage storage, DraftManager drafts, ScheduleManager schedule) {
        this.plugin = plugin;
        this.menu = menu;
        this.shows = shows;
        this.storage = storage;
        this.drafts = drafts;
        this.schedule = schedule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (!player.hasPermission("fireworksplus.use")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            menu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

        if (sub.equals("reload")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadConfig();
            storage.reload();
            schedule.reload();
            player.sendMessage(ChatColor.GREEN + "Reloaded config and data files.");
            return true;
        }

        // /fw delete <name>
        if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
                return true;
            }
            if (!p.hasPermission("fireworksplus.admin")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            String name = args[1];

            boolean ok = storage.deleteCustomShow(name);
            if (ok) {
                p.sendMessage(ChatColor.GREEN + "Deleted custom show: " + ChatColor.WHITE + name);
            } else {
                p.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + name);
            }
            return true;
        }

        if (sub.equals("list")) {
            sendShowList(player);
            return true;
        }

        if (sub.equals("stop")) {
            boolean stopped = shows.stopShow(player);
            player.sendMessage(stopped ? ChatColor.GREEN + "Show stopped." : ChatColor.RED + "No running show.");
            return true;
        }

        // /fw play <show>
        if (sub.equals("play")) {
            if (args.length == 1) {
                sendShowList(player);
                player.sendMessage(ChatColor.GRAY + "Usage: " + ChatColor.WHITE + "/fw play <show>");
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            // built-in
            if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
                String reason = shows.playShow(player, showId);
                if (reason != null) {
                    player.sendMessage(ChatColor.RED + reason);
                    return true;
                }
                sendBuiltInPreview(player, showId);
                return true;
            }

            // custom
            DraftShow custom = storage.loadCustomShow(showId);
            String reason = shows.playCustom(player, custom);
            if (reason != null) {
                player.sendMessage(ChatColor.RED + reason);
                return true;
            }
            player.sendMessage(ChatColor.AQUA + "Starting: " + ChatColor.WHITE + "Custom show");
            player.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + showId);
            return true;
        }

        // ---- Custom show builder ----

        // /fw create <name>
        if (sub.equals("create")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw create <name>");
                return true;
            }
            DraftShow d = drafts.create(player, args[1]);
            player.sendMessage(ChatColor.GREEN + "Draft created: " + ChatColor.WHITE + d.name);
            player.sendMessage(ChatColor.AQUA + "Next: " + ChatColor.WHITE + "/fw addpoint");
            return true;
        }

        // /fw addpoint
        if (sub.equals("addpoint")) {
            DraftShow d = drafts.get(player);
            if (d == null) {
                player.sendMessage(ChatColor.RED + "No draft. Use: " + ChatColor.WHITE + "/fw create <name>");
                return true;
            }
            Location loc = player.getLocation().clone();
            loc.setYaw(0);
            loc.setPitch(0);
            d.points.add(loc);

            player.sendMessage(ChatColor.GREEN + "Point added. Total points: " + ChatColor.WHITE + d.points.size());
            return true;
        }

        // /fw duration <sec>
        if (sub.equals("duration")) {
            DraftShow d = drafts.get(player);
            if (d == null) {
                player.sendMessage(ChatColor.RED + "No draft. Use: " + ChatColor.WHITE + "/fw create <name>");
                return true;
            }
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw duration <seconds>");
                return true;
            }
            int sec;
            try { sec = Integer.parseInt(args[1]); }
            catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Seconds must be a number.");
                return true;
            }

            int max = plugin.getConfig().getInt("limits.max_duration_seconds", 60);
            sec = Math.max(5, Math.min(sec, max));
            d.durationSeconds = sec;

            player.sendMessage(ChatColor.GREEN + "Duration set: " + ChatColor.WHITE + sec + "s");
            return true;
        }

        // /fw save
        if (sub.equals("save")) {
            DraftShow d = drafts.get(player);
            if (d == null) {
                player.sendMessage(ChatColor.RED + "No draft. Use: " + ChatColor.WHITE + "/fw create <name>");
                return true;
            }
            if (d.points.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Add at least one point: " + ChatColor.WHITE + "/fw addpoint");
                return true;
            }

            storage.saveCustomShow(d, player);
            drafts.clear(player);

            player.sendMessage(ChatColor.GREEN + "Custom show saved.");
            return true;
        }

        // ---- Scheduling ----
        // /fw schedule <show> <yyyy-MM-dd> <HH:mm>
        if (sub.equals("schedule")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length != 4) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw schedule <show> <yyyy-MM-dd> <HH:mm>");
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            String err = schedule.addSchedule(showId, player.getLocation(), args[2], args[3]);
            if (err != null) {
                player.sendMessage(ChatColor.RED + err);
                return true;
            }

            player.sendMessage(ChatColor.GREEN + "Scheduled show: " + ChatColor.WHITE + showId);
            return true;
        }

        // /fw unschedule <id>
        if (sub.equals("unschedule")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw unschedule <id>");
                return true;
            }

            boolean ok = schedule.removeSchedule(args[1]);
            if (ok) {
                player.sendMessage(ChatColor.GREEN + "Schedule removed: " + ChatColor.WHITE + args[1]);
            } else {
                player.sendMessage(ChatColor.RED + "Schedule not found: " + ChatColor.WHITE + args[1]);
            }
            return true;
        }

        // /fw info <show>
        if (sub.equals("info")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw info <show>");
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            // Built-in
            if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
                sendBuiltInInfo(player, showId);
                List<String> jobs = schedule.listSchedulesForShow(showId);
                sendScheduleSection(player, jobs);
                return true;
            }

            // Custom
            DraftShow custom = storage.loadCustomShow(showId);
            sendCustomInfo(player, showId);
            List<String> jobs = schedule.listSchedulesForShow(showId);
            sendScheduleSection(player, jobs);
            return true;
        }

        // /fw schedules
        if (sub.equals("schedules")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            List<String> all = schedule.listSchedulesPretty();
            if (all.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No schedules found.");
                return true;
            }

            player.sendMessage(ChatColor.AQUA + "Schedules:");
            for (String line : all) player.sendMessage(line);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        sendUsage(player);
        return true;
    }

    // ---------- Help / usage / lists ----------

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.AQUA + "Usage:");
        player.sendMessage(ChatColor.WHITE + "/fw" + ChatColor.AQUA + " - open show menu");
        player.sendMessage(ChatColor.WHITE + "/fw play <show>" + ChatColor.AQUA + " - start a show");
        player.sendMessage(ChatColor.WHITE + "/fw list" + ChatColor.AQUA + " - list all shows");
        if (player.hasPermission("fireworksplus.admin")) {
            player.sendMessage(ChatColor.WHITE + "/fw schedule <show> <yyyy-MM-dd> <HH:mm>" + ChatColor.AQUA + " - schedule a show");
            player.sendMessage(ChatColor.WHITE + "/fw schedules" + ChatColor.AQUA + " - list schedules");
            player.sendMessage(ChatColor.WHITE + "/fw reload" + ChatColor.AQUA + " - reload config");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.RED + "=== FireworksPlus ===");
        player.sendMessage(ChatColor.WHITE + "/fw" + ChatColor.AQUA + " - open show menu");
        player.sendMessage(ChatColor.WHITE + "/fw play <show>" + ChatColor.AQUA + " - start a show");
        player.sendMessage(ChatColor.WHITE + "/fw list" + ChatColor.AQUA + " - list all shows");
        player.sendMessage(ChatColor.WHITE + "/fw stop" + ChatColor.AQUA + " - stop your current show");
        player.sendMessage(ChatColor.WHITE + "/fw create <name>" + ChatColor.AQUA + " - create custom show draft");
        player.sendMessage(ChatColor.WHITE + "/fw addpoint" + ChatColor.AQUA + " - add point to draft (multiple)");
        player.sendMessage(ChatColor.WHITE + "/fw duration <sec>" + ChatColor.AQUA + " - set draft duration");
        player.sendMessage(ChatColor.WHITE + "/fw save" + ChatColor.AQUA + " - save draft to shows.yml");
        if (player.hasPermission("fireworksplus.admin")) {
            player.sendMessage(ChatColor.WHITE + "/fw schedule <show> <yyyy-MM-dd> <HH:mm>" + ChatColor.AQUA + " - schedule show at your location");
            player.sendMessage(ChatColor.WHITE + "/fw unschedule <id>" + ChatColor.AQUA + " - remove a scheduled show");
            player.sendMessage(ChatColor.WHITE + "/fw schedules" + ChatColor.AQUA + " - list schedules");
            player.sendMessage(ChatColor.WHITE + "/fw reload" + ChatColor.AQUA + " - reload config");
        }
    }

    private void sendShowList(Player player) {
        player.sendMessage(ChatColor.AQUA + "Available shows:");

        var sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                String display = plugin.getConfig().getString("shows." + id + ".display", id);
                String displayColored = ChatColor.translateAlternateColorCodes('&', display);
                player.sendMessage(ChatColor.GRAY + "- " + displayColored + ChatColor.DARK_GRAY + " (" + id + ")");
            }
        }

        List<String> custom = storage.listCustomShows();
        for (String id : custom) {
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + "Custom: " + ChatColor.WHITE + id + ChatColor.DARK_GRAY + " (" + id + ")");
        }
    }

    private void sendBuiltInPreview(Player player, String showId) {
        String display = plugin.getConfig().getString("shows." + showId + ".display", showId);
        int fireworks = plugin.getConfig().getInt("shows." + showId + ".fireworks", 30);
        int interval = plugin.getConfig().getInt("shows." + showId + ".interval_ticks", 6);
        double seconds = (fireworks * (double) interval) / 20.0;

        String displayColored = ChatColor.translateAlternateColorCodes('&', display);

        player.sendMessage(ChatColor.AQUA + "Starting: " + ChatColor.WHITE + displayColored);
        player.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + showId
                + ChatColor.DARK_GRAY + " | Fireworks: " + ChatColor.GRAY + fireworks
                + ChatColor.DARK_GRAY + " | Interval: " + ChatColor.GRAY + interval + "t"
                + ChatColor.DARK_GRAY + " | Est: " + ChatColor.GRAY + String.format("%.1f", seconds) + "s");
    }

    // Accept ID or display name
    private String resolveShowId(String input) {
        if (input == null) return null;

        String needle = normalize(input);

        // 1) built-in ids
        var sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                if (normalize(id).equals(needle)) return id;
            }
            for (String id : sec.getKeys(false)) {
                String display = plugin.getConfig().getString("shows." + id + ".display", id);
                String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', display));
                if (normalize(plain).equals(needle)) return id;
            }
        }

        // 2) custom ids
        if (storage.hasCustom(input)) return ShowStorage.normalizeId(input);
        return null;
    }

    private String normalize(String s) {
        return s.toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
    }

    private void sendBuiltInInfo(Player player, String showId) {
        String display = plugin.getConfig().getString("shows." + showId + ".display", showId);
        int fireworks = plugin.getConfig().getInt("shows." + showId + ".fireworks", 30);
        int interval = plugin.getConfig().getInt("shows." + showId + ".interval_ticks", 6);
        double radius = plugin.getConfig().getDouble("shows." + showId + ".radius", 1.6);

        player.sendMessage(ChatColor.AQUA + "=== Show Info ===");
        player.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.WHITE + "Built-in");
        player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', display));
        player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + showId);
        player.sendMessage(ChatColor.GRAY + "Fireworks: " + ChatColor.WHITE + fireworks);
        player.sendMessage(ChatColor.GRAY + "Interval: " + ChatColor.WHITE + interval + "t");
        player.sendMessage(ChatColor.GRAY + "Radius: " + ChatColor.WHITE + radius);
    }

    private void sendCustomInfo(Player player, String showId) {
        String base = "custom." + showId;

        String createdBy = storage.getYaml().getString(base + ".createdByName", "unknown");
        String createdAt = storage.getYaml().getString(base + ".createdAt", "unknown");

        List<?> pts = storage.getYaml().getList(base + ".points", List.of());

        player.sendMessage(ChatColor.AQUA + "=== Show Info ===");
        player.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.WHITE + "Custom");
        player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + showId);
        player.sendMessage(ChatColor.GRAY + "Points: " + ChatColor.WHITE + pts.size());
        player.sendMessage(ChatColor.GRAY + "Created by: " + ChatColor.WHITE + createdBy);
        player.sendMessage(ChatColor.GRAY + "Created at: " + ChatColor.WHITE + createdAt);

        // print up to 5 points (avoid spam)
        int printed = 0;
        for (Object o : pts) {
            if (!(o instanceof java.util.Map<?, ?> m)) continue;
            String w = String.valueOf(m.get("world"));
            int x = (int) Math.round(Double.parseDouble(String.valueOf(m.get("x"))));
            int y = (int) Math.round(Double.parseDouble(String.valueOf(m.get("y"))));
            int z = (int) Math.round(Double.parseDouble(String.valueOf(m.get("z"))));

            player.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + w + " " + x + " " + y + " " + z);
            printed++;
            if (printed >= 5) break;
        }
        if (pts.size() > 5) {
            player.sendMessage(ChatColor.DARK_GRAY + "... (" + (pts.size() - 5) + " more)");
        }
    }

    private void sendScheduleSection(Player player, List<String> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Schedules: " + ChatColor.DARK_GRAY + "none");
            return;
        }
        player.sendMessage(ChatColor.AQUA + "Schedules:");
        for (String line : jobs) player.sendMessage(line);
    }
}
