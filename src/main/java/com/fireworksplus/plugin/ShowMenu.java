package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShowMenu implements Listener {

    private final JavaPlugin plugin;
    private final ShowService shows;
    private final ShowStorage storage;
    private final BuilderMenu builderMenu;

    private MainMenu mainMenu;

    private final NamespacedKey keyShowId;
    private final Map<UUID, Integer> openPages = new ConcurrentHashMap<>();

    public ShowMenu(JavaPlugin plugin, ShowService shows, ShowStorage storage, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.shows = shows;
        this.storage = storage;
        this.builderMenu = builderMenu;
        this.keyShowId = new NamespacedKey(plugin, "show_id");
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public void open(Player p) {
        open(p, 0);
    }

    public boolean editCustom(Player p, String showId) {
        DraftShow custom = storage.loadCustomShow(showId);
        if (custom == null) return false;
        builderMenu.openForEdit(p, custom);
        return true;
    }

    public void open(Player p, int page) {
        String title = color(plugin.getConfig().getString("gui.shows.title", plugin.getConfig().getString("gui.title", "&cShows")));
        int size = clampSize(plugin.getConfig().getInt("gui.shows.size", plugin.getConfig().getInt("gui.size", 27)));

        Inventory inv = Bukkit.createInventory(p, size, title);

        int builderSlot = plugin.getConfig().getInt("gui.shows.builder_slot", plugin.getConfig().getInt("gui.builder_slot", 22));
        int backSlot = plugin.getConfig().getInt("gui.shows.back_slot", 26);
        int prevSlot = 21;
        int nextSlot = 23;

        List<String> showIds = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) showIds.addAll(sec.getKeys(false));
        showIds.addAll(storage.listCustomShows());
        showIds.sort(Comparator.comparing(String::toLowerCase));

        int perPage = 18;
        int totalPages = Math.max(1, (int) Math.ceil(showIds.size() / (double) perPage));
        int safePage = Math.min(Math.max(0, page), totalPages - 1);
        openPages.put(p.getUniqueId(), safePage);

        if (backSlot >= 0 && backSlot < inv.getSize()) {
            inv.setItem(backSlot, button(Material.ARROW, ChatColor.AQUA + "Back",
                    List.of(ChatColor.GRAY + "Return to main menu")));
        }

        if (builderSlot >= 0 && builderSlot < inv.getSize()) {
            inv.setItem(builderSlot, makeBuilderButton());
        }

        if (safePage > 0 && prevSlot < inv.getSize()) {
            inv.setItem(prevSlot, button(Material.ARROW, ChatColor.AQUA + "Previous Page",
                    List.of(ChatColor.GRAY + "Go to page " + safePage)));
        }

        if (safePage < totalPages - 1 && nextSlot < inv.getSize()) {
            inv.setItem(nextSlot, button(Material.ARROW, ChatColor.AQUA + "Next Page",
                    List.of(ChatColor.GRAY + "Go to page " + (safePage + 2))));
        }

        int start = safePage * perPage;
        int end = Math.min(showIds.size(), start + perPage);
        int slot = 0;
        for (int i = start; i < end; i++) {
            if (slot > 17) break;
            inv.setItem(slot, makeShowItem(showIds.get(i)));
            slot++;
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = color(plugin.getConfig().getString("gui.shows.title", plugin.getConfig().getString("gui.title", "&cShows")));
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;

        int builderSlot = plugin.getConfig().getInt("gui.shows.builder_slot", plugin.getConfig().getInt("gui.builder_slot", 22));
        int backSlot = plugin.getConfig().getInt("gui.shows.back_slot", 26);
        int prevSlot = 21;
        int nextSlot = 23;
        int page = openPages.getOrDefault(p.getUniqueId(), 0);

        if (raw == backSlot) {
            p.closeInventory();
            if (mainMenu != null) {
                Bukkit.getScheduler().runTask(plugin, () -> mainMenu.open(p));
            }
            return;
        }

        if (raw == prevSlot && page > 0) {
            open(p, page - 1);
            return;
        }

        if (raw == nextSlot) {
            open(p, page + 1);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (raw == builderSlot) {
            p.closeInventory();

            if (!hasPermission(p, "fireworksplus.builder")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return;
            }

            builderMenu.open(p);
            return;
        }

        String showId = readShowId(clicked);
        if (showId == null || showId.isBlank()) return;

        boolean isBuiltIn = plugin.getConfig().isConfigurationSection("shows." + showId);

        if (!isBuiltIn && e.getClick() == ClickType.SHIFT_RIGHT) {
            if (!hasPermission(p, "fireworksplus.admin.delete")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return;
            }

            boolean ok = storage.deleteCustomShow(showId);
            if (ok) {
                p.sendMessage(ChatColor.GREEN + "Deleted custom show: " + ChatColor.WHITE + showId);
                open(p, page); // refresh
            } else {
                p.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + showId);
            }
            return;
        }

        if (!isBuiltIn && e.getClick() == ClickType.RIGHT) {
            if (!hasPermission(p, "fireworksplus.builder")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return;
            }
            DraftShow custom = storage.loadCustomShow(showId);
            if (custom == null) {
                p.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + showId);
                return;
            }
            builderMenu.openForEdit(p, custom);
            return;
        }

        if (isBuiltIn) {
            String err = shows.playShow(p, showId);
            if (err != null) p.sendMessage(ChatColor.RED + err);
        } else {
            DraftShow custom = storage.loadCustomShow(showId);
            String err = shows.playCustom(p, custom);
            if (err != null) p.sendMessage(ChatColor.RED + err);
        }
    }

    private ItemStack makeBuilderButton() {
        ItemStack it = new ItemStack(Material.ANVIL);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Show Builder");
            meta.setLore(List.of(ChatColor.GRAY + "Create a custom show via GUI"));
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean hasPermission(Player p, String node) {
        return p.hasPermission("fireworksplus.*") || p.hasPermission(node);
    }

    private ItemStack makeShowItem(String showId) {
        boolean builtIn = plugin.getConfig().isConfigurationSection("shows." + showId);

        String display;
        List<String> lore = new ArrayList<>();

        if (builtIn) {
            display = color(plugin.getConfig().getString("shows." + showId + ".display", "&b" + showId));
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + "Built-in");
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + showId);
            lore.add(ChatColor.DARK_GRAY + "Click to play");
        } else {
            display = ChatColor.AQUA + showId;
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + "Custom");
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + showId);
            lore.add(ChatColor.DARK_GRAY + "Click to play");
            lore.add(ChatColor.DARK_GRAY + "Right-click: edit");
            lore.add(ChatColor.DARK_GRAY + "Shift+Right: delete");
        }

        Material mat = builtIn ? Material.FIREWORK_STAR : Material.NETHER_STAR;
        ItemStack it = new ItemStack(mat);

        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(display);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyShowId, PersistentDataType.STRING, showId);

            it.setItemMeta(meta);
        }

        return it;
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

    private String readShowId(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(keyShowId, PersistentDataType.STRING);
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
