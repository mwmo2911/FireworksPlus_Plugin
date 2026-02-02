package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.List;

public class BuilderColorsMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "" + "Palette Editor";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;

    public BuilderColorsMenu(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        Inventory inv = Bukkit.createInventory(p, 27, TITLE);

        preset(inv, 10, Material.RED_DYE,        ChatColor.RED + "Red",            "#ff3333", s);
        preset(inv, 11, Material.ORANGE_DYE,     ChatColor.GOLD + "Orange",        "#ff8800", s);
        preset(inv, 12, Material.YELLOW_DYE,     ChatColor.YELLOW + "Yellow",      "#ffee33", s);
        preset(inv, 13, Material.LIME_DYE,       ChatColor.GREEN + "Lime",         "#33ff66", s);
        preset(inv, 14, Material.LIGHT_BLUE_DYE, ChatColor.AQUA + "Aqua",          "#33ccff", s);
        preset(inv, 15, Material.BLUE_DYE,       ChatColor.BLUE + "Blue",          "#3355ff", s);
        preset(inv, 16, Material.PURPLE_DYE,     ChatColor.DARK_PURPLE + "Purple", "#aa33ff", s);

        preset(inv, 3, Material.WHITE_DYE,      ChatColor.WHITE + "White",        "#ffffff", s);
        preset(inv, 4, Material.GRAY_DYE,       ChatColor.GRAY + "Gray",          "#888888", s);
        preset(inv, 5, Material.BLACK_DYE,      ChatColor.DARK_GRAY + "Black",    "#111111", s);

        inv.setItem(22, button(Material.BOOK, ChatColor.AQUA + "Current Palette",
                List.of(
                        ChatColor.GRAY + "Colors: " + ChatColor.WHITE + s.palette.size(),
                        ChatColor.DARK_GRAY + "Click adds, Shift-click removes"
                )));

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + "Back",
                List.of(ChatColor.GRAY + "Return to builder")));

        p.openInventory(inv);
    }

    private void preset(Inventory inv, int slot, Material mat, String name, String hex, BuilderSession s) {
        boolean has = s.palette.stream().anyMatch(x -> x.equalsIgnoreCase(hex));
        String status = has ? (ChatColor.GREEN + "IN PALETTE") : (ChatColor.RED + "NOT IN PALETTE");

        inv.setItem(slot, button(mat, name,
                List.of(
                        ChatColor.GRAY + "Hex: " + ChatColor.WHITE + hex,
                        ChatColor.GRAY + "Status: " + status,
                        ChatColor.DARK_GRAY + "Click: add",
                        ChatColor.DARK_GRAY + "Shift-click: remove"
                )));
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

        if (slot == 26) {
            builderMenu.open(p);
            return;
        }

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        String hex = null;
        for (String line : meta.getLore()) {
            if (line == null) continue;
            String plain = ChatColor.stripColor(line);
            if (plain != null && plain.toLowerCase().startsWith("hex:")) {
                hex = plain.substring(4).trim();
                break;
            }
        }
        if (hex == null) return;
        if (!hex.matches("#?[0-9a-fA-F]{6}")) return;
        if (!hex.startsWith("#")) hex = "#" + hex;

        final String hexFinal = hex;

        BuilderSession s = builderManager.getOrCreate(p);

        boolean shift = (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT);

        if (shift) {
            boolean removed = s.palette.removeIf(x -> x.equalsIgnoreCase(hexFinal));
            if (removed) {
                p.sendMessage(ChatColor.YELLOW + "Removed color: " + ChatColor.WHITE + hexFinal);
            } else {
                p.sendMessage(ChatColor.GRAY + "That color is not in your palette.");
            }
        } else {
            if (s.palette.stream().noneMatch(x -> x.equalsIgnoreCase(hexFinal))) {
                s.palette.add(hexFinal);
                p.sendMessage(ChatColor.GREEN + "Added color: " + ChatColor.WHITE + hexFinal);
            } else {
                p.sendMessage(ChatColor.GRAY + "Already in palette: " + ChatColor.WHITE + hexFinal);
            }
        }
        open(p);
    }
}
