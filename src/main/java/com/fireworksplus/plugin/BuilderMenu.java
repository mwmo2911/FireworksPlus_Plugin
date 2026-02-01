package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BuilderMenu implements Listener {

    private static final String TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Show Builder";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final ShowStorage storage;

    private BuilderChatListener chatListener; // injected after creation
    private BuilderColorsMenu colorsMenu;     // injected after creation
    private ShowMenu showMenu;               // injected after creation

    public BuilderMenu(JavaPlugin plugin, BuilderManager builderManager, ShowStorage storage) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.storage = storage;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void setChatListener(BuilderChatListener chatListener) {
        this.chatListener = chatListener;
    }

    public void setColorsMenu(BuilderColorsMenu colorsMenu) {
        this.colorsMenu = colorsMenu;
    }

    public void setShowMenu(ShowMenu showMenu) {
        this.showMenu = showMenu;
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        Inventory inv = Bukkit.createInventory(p, 27, TITLE);

        inv.setItem(10, button(Material.NAME_TAG,
                ChatColor.AQUA + "Name: " + ChatColor.WHITE + s.name,
                List.of(ChatColor.GRAY + "Click to set via chat")));

        inv.setItem(11, button(Material.BLAZE_ROD,
                ChatColor.AQUA + "Add Point",
                List.of(ChatColor.GRAY + "Adds your current location",
                        ChatColor.DARK_GRAY + "Points: " + ChatColor.GRAY + s.points.size())));

        inv.setItem(12, button(Material.BARRIER,
                ChatColor.RED + "Remove Last Point",
                List.of(ChatColor.GRAY + "Removes the most recent point",
                        ChatColor.DARK_GRAY + "Points: " + ChatColor.GRAY + s.points.size())));

        inv.setItem(13, button(Material.CLOCK,
                ChatColor.AQUA + "Duration: " + ChatColor.WHITE + s.durationSeconds + "s",
                List.of(ChatColor.GRAY + "Left: +5s  |  Right: -5s")));

        inv.setItem(14, button(Material.REPEATER,
                ChatColor.AQUA + "Interval: " + ChatColor.WHITE + s.intervalTicks + "t",
                List.of(ChatColor.GRAY + "Left: +1t  |  Right: -1t")));

        inv.setItem(15, button(Material.FEATHER,
                ChatColor.AQUA + "Radius: " + ChatColor.WHITE + String.format("%.1f", s.radius),
                List.of(ChatColor.GRAY + "Left: +0.2  |  Right: -0.2")));

        inv.setItem(16, button(Material.GUNPOWDER,
                ChatColor.AQUA + "Power: " + ChatColor.WHITE + "min=" + s.powerMin + " max=" + s.powerMax,
                List.of(
                        ChatColor.GRAY + "Firework flight height",
                        ChatColor.GRAY + "Left: max +1 | Right: max -1",
                        ChatColor.GRAY + "Shift+Left: min +1 | Shift+Right: min -1"
                )));

        inv.setItem(21, button(Material.FIREWORK_STAR,
                ChatColor.AQUA + "Palette: " + ChatColor.WHITE + s.palette.size(),
                List.of(ChatColor.GRAY + "Click to edit colors")));

        inv.setItem(22, button(Material.EMERALD,
                ChatColor.GREEN + "Save Show",
                List.of(ChatColor.GRAY + "Saves to shows.yml",
                        ChatColor.DARK_GRAY + "Requires: name + 1 point")));

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + "Back",
                List.of(ChatColor.GRAY + "Return to main menu")));

        p.openInventory(inv);
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

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        BuilderSession s = builderManager.getOrCreate(p);

        if (slot == 26) { // back
            p.closeInventory();
            if (showMenu != null) {
                Bukkit.getScheduler().runTask(plugin, () -> showMenu.open(p));
            }
            return;
        }

        if (slot == 10) { // set name
            p.closeInventory();
            p.sendMessage(ChatColor.AQUA + "Type the show name in chat (max 24 chars).");
            if (chatListener != null) chatListener.requestName(p);
            return;
        }

        if (slot == 11) { // add point
            Location loc = p.getLocation().clone();
            loc.setYaw(0);
            loc.setPitch(0);
            s.points.add(loc);
            p.sendMessage(ChatColor.GREEN + "Point added. Total: " + ChatColor.WHITE + s.points.size());
            open(p);
            return;
        }

        if (slot == 12) { // remove last point
            if (!s.points.isEmpty()) {
                s.points.remove(s.points.size() - 1);
                p.sendMessage(ChatColor.YELLOW + "Last point removed. Total: " + ChatColor.WHITE + s.points.size());
            } else {
                p.sendMessage(ChatColor.RED + "No points to remove.");
            }
            open(p);
            return;
        }

        if (slot == 13) { // duration +/- 5s
            if (e.getClick() == ClickType.LEFT) s.durationSeconds += 5;
            else if (e.getClick() == ClickType.RIGHT) s.durationSeconds -= 5;

            int max = plugin.getConfig().getInt("limits.max_duration_seconds", 120);
            s.durationSeconds = clamp(s.durationSeconds, 5, max);

            open(p);
            return;
        }

        if (slot == 14) { // interval +/- 1 tick
            if (e.getClick() == ClickType.LEFT) s.intervalTicks += 1;
            else if (e.getClick() == ClickType.RIGHT) s.intervalTicks -= 1;

            int min = plugin.getConfig().getInt("limits.min_interval_ticks", 4);
            s.intervalTicks = clamp(s.intervalTicks, min, 60);

            open(p);
            return;
        }

        if (slot == 15) { // radius +/- 0.2
            if (e.getClick() == ClickType.LEFT) s.radius += 0.2;
            else if (e.getClick() == ClickType.RIGHT) s.radius -= 0.2;

            s.radius = clampDouble(s.radius, 0.5, 8.0);

            open(p);
            return;
        }

        if (slot == 16) { // power min/max
            ClickType c = e.getClick();

            if (c == ClickType.LEFT) s.powerMax++;
            else if (c == ClickType.RIGHT) s.powerMax--;
            else if (c == ClickType.SHIFT_LEFT) s.powerMin++;
            else if (c == ClickType.SHIFT_RIGHT) s.powerMin--;

            s.powerMin = clamp(s.powerMin, 0, 5);
            s.powerMax = clamp(s.powerMax, 0, 5);
            if (s.powerMax < s.powerMin) s.powerMax = s.powerMin;

            open(p);
            return;
        }

        if (slot == 21) { // palette
            if (colorsMenu == null) {
                p.sendMessage(ChatColor.RED + "Colors menu is not registered.");
                return;
            }
            colorsMenu.open(p);
            return;
        }

        if (slot == 22) { // save
            if (s.name == null || s.name.trim().isEmpty()) {
                p.sendMessage(ChatColor.RED + "Set a name first.");
                return;
            }
            if (s.points.isEmpty()) {
                p.sendMessage(ChatColor.RED + "Add at least one point.");
                return;
            }

            DraftShow d = new DraftShow(s.name);
            d.durationSeconds = s.durationSeconds;
            d.intervalTicks = s.intervalTicks;
            d.radius = s.radius;
            d.powerMin = s.powerMin;
            d.powerMax = s.powerMax;
            d.palette = new ArrayList<>(s.palette);
            d.points.addAll(s.points);

            storage.saveCustomShow(d, p);
            builderManager.clear(p);

            p.sendMessage(ChatColor.GREEN + "Custom show saved: " + ChatColor.WHITE + d.name);
            p.closeInventory();
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
