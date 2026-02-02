package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BuilderMenu implements Listener {

    private static final String TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Show Builder";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final ShowStorage storage;
    private final NamespacedKey keyPointTool;

    private BuilderChatListener chatListener;
    private BuilderColorsMenu colorsMenu;
    private BuilderTypesMenu typesMenu;
    private BuilderParticlesMenu particlesMenu;
    private MainMenu mainMenu;

    public BuilderMenu(JavaPlugin plugin, BuilderManager builderManager, ShowStorage storage) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.storage = storage;
        this.keyPointTool = new NamespacedKey(plugin, "builder_point_tool");
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

    public void setTypesMenu(BuilderTypesMenu typesMenu) {
        this.typesMenu = typesMenu;
    }

    public void setParticlesMenu(BuilderParticlesMenu particlesMenu) {
        this.particlesMenu = particlesMenu;
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        Inventory inv = Bukkit.createInventory(p, 27, TITLE);

        inv.setItem(1, button(Material.NAME_TAG,
                ChatColor.AQUA + "Name: " + ChatColor.WHITE + s.name,
                List.of(ChatColor.GRAY + "Click to set via chat")));

        inv.setItem(2, button(Material.BLAZE_ROD,
                ChatColor.AQUA + (s.collectingPoints ? "Finish Points" : "Add Points"),
                List.of(
                        ChatColor.GRAY + (s.collectingPoints
                                ? "Click to finish point setup"
                                : "Click to get a point tool"),
                        ChatColor.GRAY + "Right-click with the tool",
                        ChatColor.DARK_GRAY + "Points: " + ChatColor.GRAY + s.points.size(),
                        ChatColor.DARK_GRAY + "Collecting: " + ChatColor.GRAY + (s.collectingPoints ? "Yes" : "No")
                )));

        inv.setItem(3, button(Material.BARRIER,
                ChatColor.RED + "Remove Last Point",
                List.of(ChatColor.GRAY + "Removes the most recent point",
                        ChatColor.DARK_GRAY + "Points: " + ChatColor.GRAY + s.points.size())));

        inv.setItem(4, button(Material.CLOCK,
                ChatColor.AQUA + "Duration: " + ChatColor.WHITE + s.durationSeconds + "s",
                List.of(ChatColor.GRAY + "Left: +5s  |  Right: -5s")));

        inv.setItem(5, button(Material.REPEATER,
                ChatColor.AQUA + "Interval: " + ChatColor.WHITE + s.intervalTicks + "t",
                List.of(ChatColor.GRAY + "Left: +1t  |  Right: -1t")));

        inv.setItem(6, button(Material.FEATHER,
                ChatColor.AQUA + "Radius: " + ChatColor.WHITE + String.format("%.1f", s.radius),
                List.of(ChatColor.GRAY + "Left: +0.2  |  Right: -0.2")));

        inv.setItem(7, button(Material.GUNPOWDER,
                ChatColor.AQUA + "Power: " + ChatColor.WHITE + "min=" + s.powerMin + " max=" + s.powerMax,
                List.of(
                        ChatColor.GRAY + "Firework flight height",
                        ChatColor.GRAY + "Left: max +1 | Right: max -1",
                        ChatColor.GRAY + "Shift+Left: min +1 | Shift+Right: min -1"
                )));

        inv.setItem(12, button(Material.COMPASS,
                ChatColor.AQUA + "Type: " + ChatColor.WHITE + displayTypes(s.fireworkTypes),
                List.of(ChatColor.GRAY + "Click to edit types")));

        inv.setItem(13, button(Material.INK_SAC,
                ChatColor.AQUA + "Palette: " + ChatColor.WHITE + s.palette.size(),
                paletteLore(s.palette),
                paletteIcon()));

        inv.setItem(14, button(Material.GLOWSTONE_DUST,
                ChatColor.AQUA + "Particles: " + ChatColor.WHITE + displayParticles(s),
                List.of(ChatColor.GRAY + "Click to edit particles")));

        inv.setItem(25, button(Material.EMERALD,
                ChatColor.GREEN + "Save Show",
                List.of(ChatColor.GRAY + "Saves your custom show to the list",
                        ChatColor.DARK_GRAY + "Requires: name + 1 point")));

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + "Back",
                List.of(ChatColor.GRAY + "Return to main menu")));

        p.openInventory(inv);
    }

    public void openForEdit(Player p, DraftShow show) {
        builderManager.startEditing(p, show);
        open(p);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        BuilderSession s = builderManager.getOrCreate(p);

        if (slot == 26) {
            p.closeInventory();
            if (mainMenu != null) {
                Bukkit.getScheduler().runTask(plugin, () -> mainMenu.open(p));
            } else {
                p.sendMessage(ChatColor.RED + "Main menu is not registered.");
            }
            return;
        }

        if (slot == 1) {
            p.closeInventory();
            p.sendMessage(ChatColor.AQUA + "Type the show name in chat (max 24 chars).");
            if (chatListener != null) chatListener.requestName(p);
            return;
        }

        if (slot == 2) {
            if (!s.collectingPoints) {
                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(ChatColor.RED + "Your inventory is full. Empty a slot to receive the point tool.");
                    return;
                }
                p.getInventory().addItem(pointTool());
                s.collectingPoints = true;
                p.closeInventory();
                p.sendMessage(ChatColor.AQUA + "Point tool given.");
                p.sendMessage(ChatColor.GRAY + "Right-click with the tool to add points.");
                p.sendMessage(ChatColor.GRAY + "Open the builder and click \"Finish Points\" when done.");
            } else {
                removePointTools(p);
                s.collectingPoints = false;
                p.sendMessage(ChatColor.YELLOW + "Point setup finished. Total: " + ChatColor.WHITE + s.points.size());
                open(p);
            }
            return;
        }

        if (slot == 3) {
            if (!s.points.isEmpty()) {
                s.points.remove(s.points.size() - 1);
                p.sendMessage(ChatColor.YELLOW + "Last point removed. Total: " + ChatColor.WHITE + s.points.size());
            } else {
                p.sendMessage(ChatColor.RED + "No points to remove.");
            }
            open(p);
            return;
        }

        if (slot == 4) {
            if (e.getClick() == ClickType.LEFT) s.durationSeconds += 5;
            else if (e.getClick() == ClickType.RIGHT) s.durationSeconds -= 5;

            int max = plugin.getConfig().getInt("limits.max_duration_seconds", 120);
            s.durationSeconds = clamp(s.durationSeconds, 5, max);

            open(p);
            return;
        }

        if (slot == 5) {
            if (e.getClick() == ClickType.LEFT) s.intervalTicks += 1;
            else if (e.getClick() == ClickType.RIGHT) s.intervalTicks -= 1;

            int min = plugin.getConfig().getInt("limits.min_interval_ticks", 4);
            s.intervalTicks = clamp(s.intervalTicks, min, 60);

            open(p);
            return;
        }

        if (slot == 6) {
            if (e.getClick() == ClickType.LEFT) s.radius += 0.2;
            else if (e.getClick() == ClickType.RIGHT) s.radius -= 0.2;

            s.radius = clampDouble(s.radius, 0.5, 8.0);

            open(p);
            return;
        }

        if (slot == 7) {
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

        if (slot == 12) {
            if (typesMenu == null) {
                p.sendMessage(ChatColor.RED + "Types menu is not registered.");
                return;
            }
            typesMenu.open(p);
            return;
        }

        if (slot == 13) {
            if (colorsMenu == null) {
                p.sendMessage(ChatColor.RED + "Colors menu is not registered.");
                return;
            }
            colorsMenu.open(p);
            return;
        }

        if (slot == 14) {
            if (particlesMenu == null) {
                p.sendMessage(ChatColor.RED + "Particles menu is not registered.");
                return;
            }
            particlesMenu.open(p);
            return;
        }

        if (slot == 25) {
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
            d.fireworkTypes = new ArrayList<>(s.fireworkTypes);
            d.palette = new ArrayList<>(s.palette);
            d.particleTrail = s.particleTrail;
            d.trailParticles = new ArrayList<>(s.trailParticles);
            d.points.addAll(s.points);

            storage.saveCustomShow(d, p);
            builderManager.clear(p);
            removePointTools(p);

            p.sendMessage(ChatColor.GREEN + "Custom show saved: " + ChatColor.WHITE + d.name);
            p.closeInventory();
        }
    }

    @EventHandler
    public void onPointToolUse(PlayerInteractEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        BuilderSession s = builderManager.get(p);
        if (s == null || !s.collectingPoints) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        if (!isPointTool(item)) return;

        e.setCancelled(true);

        Location loc = p.getLocation().clone();
        loc.setYaw(0);
        loc.setPitch(0);
        s.points.add(loc);
        p.sendMessage(ChatColor.GREEN + "Point added. Total: " + ChatColor.WHITE + s.points.size());
        p.sendMessage(ChatColor.GRAY + "Point: " + ChatColor.WHITE + formatLocation(loc));
        spawnPointMarker(loc);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
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

    private ItemStack button(Material m, String name, List<String> lore, ItemStack base) {
        ItemStack it = base == null ? new ItemStack(m) : base;
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String displayTypes(List<String> types) {
        if (types == null || types.isEmpty()) return "Random";
        if (types.size() == 1) return types.get(0).replace("_", " ");
        return "Multiple (" + types.size() + ")";
    }

    private List<String> paletteLore(List<String> palette) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to edit colors");
        if (palette != null && !palette.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "First: " + ChatColor.GRAY + palette.get(0));
        }
        return lore;
    }

    private ItemStack paletteIcon() {
        return new ItemStack(Material.INK_SAC);
    }

    private String displayParticles(BuilderSession s) {
        if (s.trailParticles != null && !s.trailParticles.isEmpty()) {
            return String.valueOf(s.trailParticles.size());
        }
        return "None";
    }

    private ItemStack pointTool() {
        ItemStack it = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Point Tool");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Right-click to add a point",
                    ChatColor.DARK_GRAY + "Open builder to finish"
            ));
            meta.getPersistentDataContainer().set(keyPointTool, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isPointTool(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte val = pdc.get(keyPointTool, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    private void removePointTools(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.BLAZE_ROD) continue;
            if (!isPointTool(item)) continue;
            contents[i] = null;
        }
        p.getInventory().setContents(contents);

        ItemStack offhand = p.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.BLAZE_ROD && isPointTool(offhand)) {
            p.getInventory().setItemInOffHand(null);
        }
    }

    private void spawnPointMarker(Location location) {
        if (location == null || location.getWorld() == null) return;
        String particleName = plugin.getConfig().getString("particles.point_marker", "END_ROD");
        org.bukkit.Particle particle;
        try {
            particle = org.bukkit.Particle.valueOf(particleName.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 80) {
                    cancel();
                    return;
                }
                location.getWorld().spawnParticle(particle, location, 12, 0.2, 0.3, 0.2, 0.0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "";
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
