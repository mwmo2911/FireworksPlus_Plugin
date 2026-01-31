package com.fireworksplus.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FwCommand implements CommandExecutor {

    private final FireworksPlus plugin;
    private final ShowMenu menu;
    private final ShowService shows;

    public FwCommand(FireworksPlus plugin, ShowMenu menu, ShowService shows) {
        this.plugin = plugin;
        this.menu = menu;
        this.shows = shows;
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

        // /fw -> open GUI
        if (args.length == 0) {
            menu.open(player);
            return true;
        }

        // /fw help
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage(ChatColor.RED + "=== FireworksPlus ===");
            player.sendMessage(ChatColor.WHITE + "/fw" + ChatColor.AQUA + " - open show menu");
            player.sendMessage(ChatColor.WHITE + "/fw play <show>" + ChatColor.AQUA + " - start a show");
            player.sendMessage(ChatColor.WHITE + "/fw reload" + ChatColor.AQUA + " - reload config");
            return true;
        }

        // /fw play ...
        if (args.length >= 1 && args[0].equalsIgnoreCase("play")) {

            // /fw play  -> list shows
            if (args.length == 1) {
                sendShowList(player);
                player.sendMessage(ChatColor.GRAY + "Usage: " + ChatColor.WHITE + "/fw play <showId>");
                return true;
            }

            // /fw play <showId>
            if (args.length == 2) {
                String showId = args[1].toLowerCase();

                // If show not found -> error + list
                if (!plugin.getConfig().isConfigurationSection("shows." + showId)) {
                    player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + showId);
                    sendShowList(player);
                    return true;
                }

                // Preview in chat before starting
                sendShowPreview(player, showId);

                boolean ok = shows.playShow(player, showId);
                if (!ok) {
                    player.sendMessage(ChatColor.RED + "Failed to start show: " + ChatColor.WHITE + showId);
                    return true;
                }

                player.sendTitle(ChatColor.AQUA + "Show started!", ChatColor.WHITE + showId, 10, 40, 10);
                return true;
            }

            player.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/fw play <showId>");
            return true;
        }

        // /fw reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("fireworksplus.admin")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadConfig();
            player.sendMessage(ChatColor.GREEN + "Config reloaded.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand. Use: /fw help");
        return true;
    }
    private void sendShowList(Player player) {
        var sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec == null || sec.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.RED + "No shows are configured.");
            return;
        }

        player.sendMessage(ChatColor.AQUA + "Available shows:");

        // nice format: DisplayName (id)
        for (String id : sec.getKeys(false)) {
            String display = plugin.getConfig().getString("shows." + id + ".display", id);
            String displayColored = ChatColor.translateAlternateColorCodes('&', display);

            player.sendMessage(ChatColor.GRAY + "- " + displayColored + ChatColor.DARK_GRAY + " (" + id + ")");
        }
    }

    private void sendShowPreview(Player player, String showId) {
        String display = plugin.getConfig().getString("shows." + showId + ".display", showId);
        int fireworks = plugin.getConfig().getInt("shows." + showId + ".fireworks", 30);
        int interval = plugin.getConfig().getInt("shows." + showId + ".interval_ticks", 6);

        // rough duration estimate (seconds) = (fireworks * intervalTicks) / 20
        double seconds = (fireworks * (double) interval) / 20.0;

        String displayColored = ChatColor.translateAlternateColorCodes('&', display);

        player.sendMessage(ChatColor.AQUA + "Starting: " + ChatColor.WHITE + displayColored);
        player.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + showId
                + ChatColor.DARK_GRAY + " | Fireworks: " + ChatColor.GRAY + fireworks
                + ChatColor.DARK_GRAY + " | Interval: " + ChatColor.GRAY + interval + "t"
                + ChatColor.DARK_GRAY + " | Est: " + ChatColor.GRAY + String.format("%.1f", seconds) + "s");
    }
}
