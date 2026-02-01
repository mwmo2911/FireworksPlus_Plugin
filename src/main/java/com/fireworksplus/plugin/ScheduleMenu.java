package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ScheduleMenu implements Listener {

    private final JavaPlugin plugin;
    private final ScheduleManager scheduleManager;
    private MainMenu mainMenu;
    private final NamespacedKey keyScheduleId;

    public ScheduleMenu(JavaPlugin plugin, ScheduleManager scheduleManager) {
        this.plugin = plugin;
        this.scheduleManager = scheduleManager;
        this.keyScheduleId = new NamespacedKey(plugin, "schedule_id");
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public void open(Player p) {
        if (!hasPermission(p, "fireworksplus.admin.schedule")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        String title = color(plugin.getConfig().getString("gui.schedules.title", "&bSchedules"));
        int size = clampSize(plugin.getConfig().getInt("gui.schedules.size", 27));

        Inventory inv = Bukkit.createInventory(p, size, title);
        int backSlot = plugin.getConfig().getInt("gui.schedules.back_slot", 26);

        if (backSlot >= 0 && backSlot < inv.getSize()) {
            inv.setItem(backSlot, button(Material.ARROW, ChatColor.AQUA + "Back",
                    List.of(ChatColor.GRAY + "Return to main menu")));
        }

        List<String> entries = scheduleManager.listSchedulesPretty();
        if (entries.isEmpty()) {
            int center = Math.min(inv.getSize() - 1, inv.getSize() / 2);
            inv.setItem(center, button(Material.BARRIER, ChatColor.RED + "No schedules",
                    List.of(ChatColor.GRAY + "No scheduled shows")));
            p.openInventory(inv);
            return;
        }

        int slot = 0;
        for (String entry : entries) {
            while (slot < inv.getSize() && slot == backSlot) slot++;
            if (slot >= inv.getSize()) break;
            inv.setItem(slot, scheduleItem(entry));
            slot++;
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = color(plugin.getConfig().getString("gui.schedules.title", "&bSchedules"));
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;

        int backSlot = plugin.getConfig().getInt("gui.schedules.back_slot", 26);
        if (raw == backSlot && mainMenu != null) {
            mainMenu.open(p);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String scheduleId = readScheduleId(clicked);
        if (scheduleId == null || scheduleId.isBlank()) return;

        if (!hasPermission(p, "fireworksplus.admin.schedule")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        boolean removed = scheduleManager.removeSchedule(scheduleId);
        if (removed) {
            p.sendMessage(ChatColor.GREEN + "Schedule removed: " + ChatColor.WHITE + scheduleId);
            open(p);
        } else {
            p.sendMessage(ChatColor.RED + "Schedule not found: " + ChatColor.WHITE + scheduleId);
        }
    }

    private boolean hasPermission(Player p, String node) {
        return p.hasPermission("fireworksplus.*") || p.hasPermission(node);
    }

    private ItemStack scheduleItem(String entry) {
        String plain = ChatColor.stripColor(entry);
        String[] parts = plain.split("\\s*\\|\\s*");

        String header = parts.length > 0 ? parts[0].trim() : "Schedule";
        String scheduleId = extractScheduleId(header);
        boolean done = header.contains("[DONE]");
        ChatColor statusColor = done ? ChatColor.RED : ChatColor.GREEN;

        String when = parts.length > 1 ? parts[1].trim() : "";
        String show = parts.length > 2 ? parts[2].trim() : "";
        String location = parts.length > 3 ? parts[3].trim() : "";

        List<String> lore = new ArrayList<>();
        if (!when.isBlank()) lore.add(ChatColor.GRAY + "When: " + ChatColor.WHITE + when);
        if (!show.isBlank()) lore.add(ChatColor.GRAY + "Show: " + ChatColor.WHITE + show);
        if (!location.isBlank()) lore.add(ChatColor.GRAY + "At: " + ChatColor.WHITE + location);

        lore.add(ChatColor.DARK_GRAY + "Click to remove");

        ItemStack item = button(Material.PAPER, statusColor + header, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && scheduleId != null && !scheduleId.isBlank()) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyScheduleId, PersistentDataType.STRING, scheduleId);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack button(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String readScheduleId(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(keyScheduleId, PersistentDataType.STRING);
    }

    private String extractScheduleId(String header) {
        if (header == null) return null;
        String[] parts = header.trim().split("\\s+");
        if (parts.length >= 2) return parts[1];
        return null;
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
