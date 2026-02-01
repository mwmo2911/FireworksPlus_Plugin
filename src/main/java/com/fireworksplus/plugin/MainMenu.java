package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MainMenu implements Listener {

    private final JavaPlugin plugin;
    private final ShowMenu showMenu;       // your existing show list GUI
    private final BuilderMenu builderMenu; // your builder GUI

    private final String title;

    public MainMenu(JavaPlugin plugin, ShowMenu showMenu, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.showMenu = showMenu;
        this.builderMenu = builderMenu;

        FileConfiguration c = plugin.getConfig();
        this.title = color(c.getString("main_gui.title", "&cFireworksPlus"));
    }

    public void open(Player p) {
        int size = clampSize(plugin.getConfig().getInt("main_gui.size", 27));
        Inventory inv = Bukkit.createInventory(p, size, title);

        // simple filler (optional)
        Material filler = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack fill = item(filler, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fill);

        int showsSlot = plugin.getConfig().getInt("main_gui.shows_slot", 12);
        int builderSlot = plugin.getConfig().getInt("main_gui.builder_slot", 14);

        inv.setItem(showsSlot, item(Material.FIREWORK_STAR,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Shows",
                List.of(ChatColor.GRAY + "Browse and start firework shows")));

        inv.setItem(builderSlot, item(Material.ANVIL,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Builder",
                List.of(ChatColor.GRAY + "Create custom shows")));

        // Optional admin buttons
        if (p.hasPermission("fireworksplus.admin")) {
            int reloadSlot = plugin.getConfig().getInt("main_gui.reload_slot", 22);
            inv.setItem(reloadSlot, item(Material.REDSTONE,
                    ChatColor.RED + "" + ChatColor.BOLD + "Reload",
                    List.of(ChatColor.GRAY + "Reload config and data files")));
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;

        int showsSlot = plugin.getConfig().getInt("main_gui.shows_slot", 12);
        int builderSlot = plugin.getConfig().getInt("main_gui.builder_slot", 14);
        int reloadSlot = plugin.getConfig().getInt("main_gui.reload_slot", 22);

        if (raw == showsSlot) {
            showMenu.open(p);
            return;
        }

        if (raw == builderSlot) {
            if (!p.hasPermission("fireworksplus.admin")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return;
            }
            builderMenu.open(p);
            return;
        }

        if (raw == reloadSlot && p.hasPermission("fireworksplus.admin")) {
            plugin.reloadConfig();
            p.sendMessage(ChatColor.GREEN + "Reloaded config.");
            open(p); // refresh
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private int clampSize(int size) {
        int s = Math.max(9, Math.min(54, size));
        return ((s + 8) / 9) * 9;
    }
}
