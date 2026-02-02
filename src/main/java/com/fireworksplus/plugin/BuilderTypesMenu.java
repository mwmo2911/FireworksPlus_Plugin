package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
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

public class BuilderTypesMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "" + "Type Editor";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;

    public BuilderTypesMenu(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        Inventory inv = Bukkit.createInventory(p, 27, TITLE);

        typeButton(inv, 11, FireworkEffect.Type.BALL, s);
        typeButton(inv, 12, FireworkEffect.Type.BALL_LARGE, s);
        typeButton(inv, 13, FireworkEffect.Type.STAR, s);
        typeButton(inv, 14, FireworkEffect.Type.BURST, s);
        typeButton(inv, 15, FireworkEffect.Type.CREEPER, s);

        inv.setItem(22, button(Material.BOOK, ChatColor.AQUA + "Current Types",
                List.of(
                        ChatColor.GRAY + "Selected: " + ChatColor.WHITE + s.fireworkTypes.size(),
                        ChatColor.DARK_GRAY + "Click adds, Shift-click removes"
                )));

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + "Back",
                List.of(ChatColor.GRAY + "Return to builder")));

        p.openInventory(inv);
    }

    private void typeButton(Inventory inv, int slot, FireworkEffect.Type type, BuilderSession s) {
        boolean has = s.fireworkTypes.stream().anyMatch(x -> x.equalsIgnoreCase(type.name()));
        String status = has ? (ChatColor.GREEN + "IN TYPES") : (ChatColor.RED + "NOT IN TYPES");

        inv.setItem(slot, button(materialFor(type),
                ChatColor.AQUA + display(type),
                List.of(
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
        if (meta == null) return;

        String plain = ChatColor.stripColor(meta.getDisplayName());
        if (plain == null) return;

        FireworkEffect.Type type = parseType(plain);
        if (type == null) return;

        BuilderSession s = builderManager.getOrCreate(p);
        boolean shift = (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT);

        if (shift) {
            boolean removed = s.fireworkTypes.removeIf(x -> x.equalsIgnoreCase(type.name()));
            if (removed) {
                p.sendMessage(ChatColor.YELLOW + "Removed type: " + ChatColor.WHITE + display(type));
            } else {
                p.sendMessage(ChatColor.GRAY + "That type is not selected.");
            }
        } else {
            if (s.fireworkTypes.stream().noneMatch(x -> x.equalsIgnoreCase(type.name()))) {
                s.fireworkTypes.add(type.name());
                p.sendMessage(ChatColor.GREEN + "Added type: " + ChatColor.WHITE + display(type));
            } else {
                p.sendMessage(ChatColor.GRAY + "Already selected: " + ChatColor.WHITE + display(type));
            }
        }

        open(p);
    }

    private String display(FireworkEffect.Type type) {
        return type.name().replace("_", " ");
    }

    private FireworkEffect.Type parseType(String label) {
        String normalized = label.replace(" ", "_").toUpperCase();
        try {
            return FireworkEffect.Type.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Material materialFor(FireworkEffect.Type type) {
        return switch (type) {
            case BALL -> Material.SLIME_BALL;
            case BALL_LARGE -> Material.MAGMA_CREAM;
            case STAR -> Material.NETHER_STAR;
            case BURST -> Material.FIRE_CHARGE;
            case CREEPER -> Material.CREEPER_HEAD;
        };
    }
}