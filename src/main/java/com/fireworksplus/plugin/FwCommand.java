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

    public FwCommand(
            FireworksPlus plugin,
            ShowMenu menu,
            ShowService shows,
            ShowStorage storage,
            DraftManager drafts,
            ScheduleManager schedule
    ) {
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

        /* =======================
           /fw  → open GUI
           ======================= */
        if (args.length == 0) {
            menu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        /* =======================
           /fw version
           ======================= */
        if (sub.equals("version")) {
            sendVersionBox(player);
            return true;
        }

        /* =======================
           /fw help
           ======================= */
        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }

        /* =======================
           /fw stop
           ======================= */
        if (sub.equals("stop")) {
            boolean stopped = shows.stopShow(player);
            player.sendMessage(stopped
                    ? ChatColor.GREEN + "Show stopped."
                    : ChatColor.RED + "No running show.");
            return true;
        }

        /* =======================
           /fw delete <id>
           ======================= */
        if (sub.equals("delete") && args.length == 2) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            boolean ok = storage.deleteCustomShow(args[1]);
            player.sendMessage(ok
                    ? ChatColor.GREEN + "Deleted custom show: " + ChatColor.WHITE + args[1]
                    : ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + args[1]);
            return true;
        }

        /* =======================
           fallback
           ======================= */
        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        sendHelp(player);
        return true;
    }

    /* =====================================================
       VERSION TEXT
       ===================================================== */
    private void sendVersionBox(Player p) {
        String line1 =
                ChatColor.GOLD + "FireworksPlus " +
                        ChatColor.YELLOW + "v" + plugin.getDescription().getVersion();

        String line2 =
                ChatColor.GOLD + "Created by: " +
                        ChatColor.YELLOW + "mwmo2911";

        p.sendMessage(line1);
        p.sendMessage(line2);
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.GRAY + "Commands:");
        p.sendMessage(ChatColor.YELLOW + "/fw" + ChatColor.WHITE + " - Open the GUI");
        p.sendMessage(ChatColor.YELLOW + "/fw help" + ChatColor.WHITE + " - Show this help list");
        p.sendMessage(ChatColor.YELLOW + "/fw version" + ChatColor.WHITE + " - Show plugin version");
        p.sendMessage(ChatColor.YELLOW + "/fw stop" + ChatColor.WHITE + " - Stop the running show");
        if (p.hasPermission("fireworksplus.admin")) {
            p.sendMessage(ChatColor.YELLOW + "/fw delete <id>" + ChatColor.WHITE + " - Delete a custom show");
        }
    }
}
