package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworksPlus extends JavaPlugin {

    private ShowService showService;
    private ShowMenu showMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.showService = new ShowService(this);
        this.showMenu = new ShowMenu(this, showService);

        Bukkit.getPluginManager().registerEvents(showMenu, this);

        var cmd = getCommand("fw");
        if (cmd != null) {
            cmd.setExecutor(new FwCommand(this, showMenu, showService));
        }

        getLogger().info("FireworksPlus enabled.");
    }
}
