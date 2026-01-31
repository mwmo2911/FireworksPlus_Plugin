package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworksPlus extends JavaPlugin {

    private ShowService showService;
    private ShowStorage showStorage;
    private DraftManager draftManager;
    private ScheduleManager scheduleManager;
    private ShowMenu showMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.showService = new ShowService(this);
        this.showStorage = new ShowStorage(this);
        this.draftManager = new DraftManager(this);
        this.scheduleManager = new ScheduleManager(this, showService, showStorage);
        this.showMenu = new ShowMenu(this, showService, showStorage);

        Bukkit.getPluginManager().registerEvents(showMenu, this);

        var cmd = getCommand("fw");
        if (cmd != null) {
            cmd.setExecutor(new FwCommand(this, showMenu, showService, showStorage, draftManager, scheduleManager));
        }

        scheduleManager.startPolling();

        getLogger().info("FireworksPlus enabled.");
    }
}
