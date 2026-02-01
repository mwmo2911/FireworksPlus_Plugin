package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Captures chat input when the player is in "enter name" mode.
 */
public class BuilderChatListener implements Listener {

    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;

    private final Set<UUID> waitingForName = new HashSet<>();

    public BuilderChatListener(BuilderManager builderManager, BuilderMenu builderMenu) {
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

        String msg = e.getMessage() == null ? "" : e.getMessage().trim();
        if (msg.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Name cannot be empty. Type a name in chat.");
            return;
        }

        if (msg.length() > 24) {
            p.sendMessage(ChatColor.RED + "Name is too long (max 24 chars).");
            return;
        }

        if (!msg.matches("[A-Za-z0-9 _-]+")) {
            p.sendMessage(ChatColor.RED + "Name can only contain letters, numbers, spaces, _ and -");
            return;
        }

        BuilderSession s = builderManager.getOrCreate(p);
        s.name = msg;

        waitingForName.remove(p.getUniqueId());

        p.sendMessage(ChatColor.GREEN + "Builder name set to: " + ChatColor.WHITE + msg);

        Bukkit.getScheduler().runTask(builderMenu.getPlugin(), () -> builderMenu.open(p));
    }
}
