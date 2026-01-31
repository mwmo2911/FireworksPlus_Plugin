package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ShowMenu implements Listener {

    private final JavaPlugin plugin;
    private final ShowService shows;
    private final ShowStorage storage;

    public ShowMenu(JavaPlugin plugin, ShowService shows, ShowStorage storage) {
        this.plugin = plugin;
        this.shows = shows;
        this.storage = storage;
    }

    public void open(Player p) {
        String title = color(plugin.getConfig().getString("gui.title", "&b&lFireworksPlus &7Shows"));
        int size = plugin.getConfig().getInt("gui.size", 27);
        size = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));

        Inventory inv = Bukkit.createInventory(p, size, title);

        // built-in shows
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection show = sec.getConfigurationSection(id);
                if (show == null) continue;

                int slot = show.getInt("slot", -1);
                if (slot < 0 || slot >= inv.getSize()) continue;

                String display = show.getString("display", id);

                inv.setItem(slot, buildItem(Material.FIREWORK_ROCKET, display, id, "Built-in"));
            }
        }

        // custom shows go into first free slots
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) free.add(i);

        List<String> customs = storage.listCustomShows();
        for (int i = 0; i < customs.size() && i < free.size(); i++) {
            String id = customs.get(i);
            inv.setItem(free.get(i), buildItem(Material.FIREWORK_STAR, "&aCustom: &f" + id, id, "Custom"));
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = color(plugin.getConfig().getString("gui.title", "&b&lFireworksPlus &7Shows"));
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null || meta.getLore().size() < 3) return;

        String idLine = ChatColor.stripColor(meta.getLore().get(1));
        if (!idLine.startsWith("ID: ")) return;
        String showId = idLine.substring(4).trim();

        // built-in or custom
        if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
            String reason = shows.playShow(p, showId);
            if (reason != null) {
                p.sendMessage(ChatColor.RED + reason);
                p.closeInventory();
                return;
            }
            sendBuiltInPreview(p, showId);
            p.closeInventory();
            return;
        }

        DraftShow custom = storage.loadCustomShow(showId);
        String reason = shows.playCustom(p, custom);
        if (reason != null) {
            p.sendMessage(ChatColor.RED + reason);
            p.closeInventory();
            return;
        }
        sendCustomPreview(p, showId, custom);
        p.closeInventory();
    }

    private ItemStack buildItem(Material mat, String display, String id, String type) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(display));
            meta.setLore(List.of(
                    ChatColor.AQUA + "Click to start",
                    ChatColor.DARK_GRAY + "ID: " + id,
                    ChatColor.DARK_GRAY + "Type: " + type
            ));
            it.setItemMeta(meta);
        }
        return it;
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

    private void sendCustomPreview(Player player, String showId, DraftShow custom) {
        int points = custom == null ? 0 : custom.points.size();
        int interval = custom == null ? 6 : custom.intervalTicks;

        player.sendMessage(ChatColor.AQUA + "Starting: " + ChatColor.WHITE + "Custom show");
        player.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + showId
                + ChatColor.DARK_GRAY + " | Points: " + ChatColor.GRAY + points
                + ChatColor.DARK_GRAY + " | Interval: " + ChatColor.GRAY + interval + "t");
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
