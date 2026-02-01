package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworksPlus extends JavaPlugin {

    private ShowService showService;
    private ShowStorage showStorage;
    private DraftManager draftManager;
    private ScheduleManager scheduleManager;

    // GUIs
    private ShowMenu showMenu;
    private BuilderMenu builderMenu;
    private BuilderColorsMenu builderColorsMenu;
    private BuilderTypesMenu builderTypesMenu;
    private MainMenu mainMenu;
    private ScheduleMenu scheduleMenu;

    // Builder system
    private BuilderManager builderManager;
    private BuilderChatListener builderChatListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Core services
        this.showService = new ShowService(this);
        this.showStorage = new ShowStorage(this);
        this.draftManager = new DraftManager(this);
        this.scheduleManager = new ScheduleManager(this, showService, showStorage);

        // ---------------- Builder system ----------------
        this.builderManager = new BuilderManager(this);

        this.builderMenu = new BuilderMenu(this, builderManager, showStorage);
        this.builderChatListener = new BuilderChatListener(this, builderManager, builderMenu);
        this.builderColorsMenu = new BuilderColorsMenu(this, builderManager, builderMenu);
        this.builderTypesMenu = new BuilderTypesMenu(this, builderManager, builderMenu);

        builderMenu.setChatListener(builderChatListener);
        builderMenu.setColorsMenu(builderColorsMenu);
        builderMenu.setTypesMenu(builderTypesMenu);

        // ---------------- Show menu + Main menu ----------------
        this.showMenu = new ShowMenu(this, showService, showStorage, builderMenu);
        this.scheduleMenu = new ScheduleMenu(this, scheduleManager);
        this.mainMenu = new MainMenu(this, showMenu, builderMenu, scheduleMenu, showStorage, scheduleManager);

        // Make Back buttons work
        showMenu.setMainMenu(mainMenu);
        builderMenu.setMainMenu(mainMenu);
        scheduleMenu.setMainMenu(mainMenu);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(mainMenu, this);
        Bukkit.getPluginManager().registerEvents(showMenu, this);
        Bukkit.getPluginManager().registerEvents(builderMenu, this);
        Bukkit.getPluginManager().registerEvents(builderTypesMenu, this);
        Bukkit.getPluginManager().registerEvents(scheduleMenu, this);
        Bukkit.getPluginManager().registerEvents(builderColorsMenu, this);
        Bukkit.getPluginManager().registerEvents(builderChatListener, this);

        // Commands
        PluginCommand cmd = getCommand("fw");
        if (cmd != null) {
            // IMPORTANT: FwCommand must accept MainMenu as the 2nd arg.
            cmd.setExecutor(new FwCommand(this, mainMenu, showMenu, showService, showStorage, draftManager, scheduleManager));
            cmd.setTabCompleter(new FwTabCompleter(this, showStorage, scheduleManager));
        }

        scheduleManager.startPolling();
        getLogger().info("FireworksPlus enabled.");
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }
}