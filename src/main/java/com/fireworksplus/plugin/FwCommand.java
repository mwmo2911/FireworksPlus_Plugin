package com.fireworksplus.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Main /fw command.
 * With the MainMenu, /fw (no args) opens the MainMenu.
 */
public class FwCommand implements CommandExecutor {

    private final FireworksPlus plugin;
    private final MainMenu mainMenu;
    private final ShowService shows;
    private final ShowStorage storage;
    private final DraftManager drafts;
    private final ScheduleManager schedule;

    public FwCommand(FireworksPlus plugin, MainMenu mainMenu, ShowService shows, ShowStorage storage, DraftManager drafts, ScheduleManager schedule) {
        this.plugin = plugin;
        this.mainMenu = mainMenu;
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
            mainMenu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

        if (sub.equals("version") || sub.equals("ver")) {
            sendVersionBox(player);
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
        if (sub.equals("delete")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw delete <show>");
                return true;
            }

            String name = args[1];
            boolean ok = storage.deleteCustomShow(name);

            if (ok) {
                player.sendMessage(ChatColor.GREEN + "Deleted custom show: " + ChatColor.WHITE + name);
            } else {
                player.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + name);
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

        if (sub.equals("menu")) {
            mainMenu.open(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        sendUsage(player);
        return true;
    }

    // ---------- Help / usage ----------

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.RED + "=== FireworksPlus ===");
        player.sendMessage(ChatColor.AQUA + "Usage:");
        player.sendMessage(ChatColor.WHITE + "/fw" + ChatColor.AQUA + " - open main menu");
        player.sendMessage(ChatColor.WHITE + "/fw play <show>" + ChatColor.AQUA + " - start a show");
        player.sendMessage(ChatColor.WHITE + "/fw stop" + ChatColor.AQUA + " - stop your current show");
        player.sendMessage(ChatColor.WHITE + "/fw list" + ChatColor.AQUA + " - list all shows");
        player.sendMessage(ChatColor.WHITE + "/fw version" + ChatColor.AQUA + " - plugin version");
        if (player.hasPermission("fireworksplus.admin")) {
            player.sendMessage(ChatColor.WHITE + "/fw schedule <show> <yyyy-MM-dd> <HH:mm>" + ChatColor.AQUA + " - schedule a show");
            player.sendMessage(ChatColor.WHITE + "/fw schedules" + ChatColor.AQUA + " - list schedules");
            player.sendMessage(ChatColor.WHITE + "/fw delete <show>" + ChatColor.AQUA + " - delete a custom show");
            player.sendMessage(ChatColor.WHITE + "/fw reload" + ChatColor.AQUA + " - reload config");
        }
    }

    private void sendHelp(Player player) {
        sendUsage(player);
    }

    // ---------- Version box ----------


    private void sendVersionBox(Player p) {
        String ver = plugin.getDescription().getVersion();

        // Keep it simple: Minecraft chat is not perfectly monospace, so avoid relying on trailing padding.
        // We build a fixed-width frame using only visible characters.
        List<String> plainLines = List.of(
                "FireworksPlus v" + ver,
                "Created by: mwmo2911"
        );

        int maxLen = 0;
        for (String s : plainLines) maxLen = Math.max(maxLen, s.length());

        int pad = 2; // spaces on each side inside the box
        int innerWidth = maxLen + (pad * 2);

        p.sendMessage(rainbowBorderLine('┌', '┐', innerWidth));
        for (String line : plainLines) {
            p.sendMessage(wallLine(line, innerWidth, pad));
        }
        p.sendMessage(rainbowBorderLine('└', '┘', innerWidth));
    }

    private String rainbowBorderLine(char left, char right, int innerWidth) {
        ChatColor[] rainbow = new ChatColor[] {
                ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW,
                ChatColor.GREEN, ChatColor.RED, ChatColor.BLUE
        };

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.RESET).append(rainbow[0]).append(left);

        for (int i = 0; i < innerWidth; i++) {
            sb.append(rainbow[i % rainbow.length]).append('─');
        }

        sb.append(rainbow[(innerWidth + 1) % rainbow.length]).append(right).append(ChatColor.RESET);
        return sb.toString();
    }

    private String wallLine(String plain, int innerWidth, int pad) {
        ChatColor[] rainbow = new ChatColor[] {
                ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW,
                ChatColor.GREEN, ChatColor.RED, ChatColor.BLUE
        };

        // Left padding spaces + text + filler dashes (NOT spaces) so alignment doesn't depend on trimming.
        int leftSpaces = pad;
        int remaining = innerWidth - leftSpaces - plain.length();
        if (remaining < 0) remaining = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.RESET).append(rainbow[0]).append('│');
        sb.append(ChatColor.GOLD);

        sb.append(repeatChar(' ', leftSpaces));
        sb.append(plain);

        // Filler (looks like a clean divider, and keeps the right wall stable)
        sb.append(ChatColor.DARK_GRAY).append(repeatChar('─', remaining));

        sb.append(rainbow[(innerWidth + 1) % rainbow.length]).append('│').append(ChatColor.RESET);
        return sb.toString();
    }

    private String repeatChar(char c, int n) {
        if (n <= 0) return "";
        return String.valueOf(c).repeat(n);
    }

    private String normalize(String s) {
        return s.toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
    }
}
