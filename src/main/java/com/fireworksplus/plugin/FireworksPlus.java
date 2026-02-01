package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworksPlus extends JavaPlugin {

    private ShowService showService;
    private ShowStorage showStorage;
    private DraftManager draftManager;
    private ScheduleManager scheduleManager;

    private ShowMenu showMenu;

    private BuilderManager builderManager;
    private BuilderMenu builderMenu;
    private BuilderChatListener builderChatListener;
    private BuilderColorsMenu builderColorsMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.showService = new ShowService(this);
        this.showStorage = new ShowStorage(this);
        this.draftManager = new DraftManager(this);
        this.scheduleManager = new ScheduleManager(this, showService, showStorage);

        // Builder system
        this.builderManager = new BuilderManager(this);
        this.builderMenu = new BuilderMenu(this, builderManager, showStorage);
        this.builderChatListener = new BuilderChatListener(builderManager, builderMenu);
        builderMenu.setChatListener(builderChatListener);
        this.builderColorsMenu = new BuilderColorsMenu(this, builderManager, builderMenu);
        builderMenu.setColorsMenu(builderColorsMenu);
        builderMenu.setShowMenu(showMenu);

        // Main GUI
        this.showMenu = new ShowMenu(this, showService, showStorage, builderMenu);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(showMenu, this);
        Bukkit.getPluginManager().registerEvents(builderMenu, this);
        Bukkit.getPluginManager().registerEvents(builderChatListener, this);
        Bukkit.getPluginManager().registerEvents(builderColorsMenu, this);

        // Commands (INSIDE onEnable)
        PluginCommand cmd = getCommand("fw");
        if (cmd != null) {
            cmd.setExecutor(new FwCommand(this, showMenu, showService, showStorage, draftManager, scheduleManager));
            cmd.setTabCompleter(new FwTabCompleter(this, showStorage, scheduleManager));
        }

        scheduleManager.startPolling();

        getLogger().info("FireworksPlus enabled.");
    }
}
