package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
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
import java.util.Locale;

public class BuilderParticlesMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Particle Editor";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;

    public BuilderParticlesMenu(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        Inventory inv = Bukkit.createInventory(p, 27, TITLE);
        List<String> options = particleOptions();

        int[] slots = new int[] {1, 2, 3, 4, 5, 6, 7, 12, 13, 14};
        int count = Math.min(options.size(), slots.length);
        for (int i = 0; i < count; i++) {
            particleButton(inv, slots[i], options.get(i), s);
        }

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + "Back",
                List.of(ChatColor.GRAY + "Return to builder")));

        p.openInventory(inv);
    }

    private void particleButton(Inventory inv, int slot, String particleName, BuilderSession s) {
        boolean has = s.trailParticles.stream().anyMatch(x -> x.equalsIgnoreCase(particleName));
        String status = has ? (ChatColor.GREEN + "IN LIST") : (ChatColor.RED + "NOT IN LIST");

        inv.setItem(slot, button(materialForParticle(particleName),
                ChatColor.AQUA + display(particleName),
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

        BuilderSession s = builderManager.getOrCreate(p);

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;

        String plain = ChatColor.stripColor(meta.getDisplayName());
        if (plain == null) return;

        String particleName = parseParticleName(plain);
        if (particleName == null) return;

        boolean shift = (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT);
        if (shift) {
            boolean removed = s.trailParticles.removeIf(x -> x.equalsIgnoreCase(particleName));
            if (removed) {
                p.sendMessage(ChatColor.YELLOW + "Removed particle: " + ChatColor.WHITE + display(particleName));
            } else {
                p.sendMessage(ChatColor.GRAY + "That particle is not selected.");
            }
        } else {
            if (s.trailParticles.stream().noneMatch(x -> x.equalsIgnoreCase(particleName))) {
                s.trailParticles.add(particleName);
                s.particleTrail = true;
                p.sendMessage(ChatColor.GREEN + "Added particle: " + ChatColor.WHITE + display(particleName));
            } else {
                p.sendMessage(ChatColor.GRAY + "Already selected: " + ChatColor.WHITE + display(particleName));
            }
        }

        open(p);
    }

    private String display(String particleName) {
        return particleName.replace("_", " ");
    }

    private String parseParticleName(String label) {
        String normalized = label.replace(" ", "_").toUpperCase(Locale.ROOT);
        try {
            Particle.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Material materialForParticle(String particleName) {
        if (particleName == null) return Material.GLOWSTONE_DUST;
        String key = particleName.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "DRIP_WATER", "WATER_SPLASH", "WATER_WAKE", "WATER_BUBBLE" -> Material.WATER_BUCKET;
            case "DRIP_LAVA", "LAVA" -> Material.LAVA_BUCKET;
            case "HEART" -> Material.POPPY;
            case "END_ROD" -> Material.END_ROD;
            case "VILLAGER_HAPPY" -> Material.EMERALD;
            case "CRIT" -> Material.IRON_NUGGET;
            case "CLOUD" -> Material.WHITE_DYE;
            case "ENCHANT" -> Material.ENCHANTING_TABLE;
            case "FIREWORKS_SPARK" -> Material.FIREWORK_STAR;
            default -> Material.GLOWSTONE_DUST;
        };
    }

    private List<String> particleOptions() {
        List<String> configured = plugin.getConfig().getStringList("particles.options");
        List<String> options = new ArrayList<>();
        if (configured != null) {
            for (String entry : configured) {
                if (entry == null || entry.isBlank()) continue;
                String normalized = entry.trim().toUpperCase(Locale.ROOT);
                try {
                    Particle.valueOf(normalized);
                    options.add(normalized);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
        }

        if (!options.isEmpty()) {
            return options;
        }

        List<String> fallback = List.of("FIREWORKS_SPARK", "END_ROD", "VILLAGER_HAPPY", "CRIT", "CLOUD", "ENCHANT");
        List<String> filtered = new ArrayList<>();
        for (String entry : fallback) {
            try {
                Particle.valueOf(entry);
                filtered.add(entry);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
        }
        return filtered;
    }
}
