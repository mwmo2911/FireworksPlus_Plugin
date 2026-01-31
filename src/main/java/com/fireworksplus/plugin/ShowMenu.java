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

import java.util.List;

public class ShowMenu implements Listener {

    private final JavaPlugin plugin;
    private final ShowService shows;

    public ShowMenu(JavaPlugin plugin, ShowService shows) {
        this.plugin = plugin;
        this.shows = shows;
    }

    public void open(Player p) {
        String title = color(plugin.getConfig().getString("gui.title", "&b&lFireworksPlus &7Shows"));
        int size = plugin.getConfig().getInt("gui.size", 27);
        size = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));

        Inventory inv = Bukkit.createInventory(p, size, title);

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection show = sec.getConfigurationSection(id);
                if (show == null) continue;

                int slot = show.getInt("slot", -1);
                String display = show.getString("display", id);

                ItemStack it = new ItemStack(Material.FIREWORK_ROCKET);
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(color(display));
                    meta.setLore(List.of(
                            ChatColor.AQUA + "Click to start",
                            ChatColor.DARK_GRAY + "ID: " + id
                    ));
                    it.setItemMeta(meta);
                }

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, it);
                }
            }
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
        if (meta == null || meta.getLore() == null) return;

        // We stored "ID: <id>" in lore line 2
        String idLine = ChatColor.stripColor(meta.getLore().get(1));
        if (!idLine.startsWith("ID: ")) return;

        String showId = idLine.substring(4).trim();

        boolean ok = shows.playShow(p, showId);
        if (!ok) {
            p.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + showId);
        } else {
            p.sendTitle(ChatColor.AQUA + "Show started!", ChatColor.WHITE + showId, 10, 40, 10);
        }

        p.closeInventory();
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
