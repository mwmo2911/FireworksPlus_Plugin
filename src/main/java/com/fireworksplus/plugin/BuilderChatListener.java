package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuilderChatListener implements Listener {

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;

    private final Set<UUID> waitingForName = new HashSet<>();

    public BuilderChatListener(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
    }

    public void requestName(Player p) {
        waitingForName.add(p.getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!waitingForName.contains(p.getUniqueId())) return;

        e.setCancelled(true);

        String msg = e.getMessage().trim();

        if (msg.isEmpty() || msg.length() > 24) {
            p.sendMessage(ChatColor.RED + "Name must be 1-24 characters.");
            return;
        }
        if (!msg.matches("[A-Za-z0-9 _\\-]+")) {
            p.sendMessage(ChatColor.RED + "Name can only contain letters, numbers, spaces, _ and -.");
            return;
        }

        BuilderSession s = builderManager.getOrCreate(p);
        s.name = msg;

        waitingForName.remove(p.getUniqueId());

        p.sendMessage(ChatColor.GREEN + "Builder name set to: " + ChatColor.WHITE + msg);

        Bukkit.getScheduler().runTask(plugin, () -> builderMenu.open(p));
    }
}
