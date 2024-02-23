package fr.palmus.mobscontroller;

import fr.palmus.mobscontroller.config.RegionConfig;
import fr.palmus.mobscontroller.events.DamageManager;
import fr.palmus.mobscontroller.executor.MobscCompleter;
import fr.palmus.mobscontroller.executor.MobscExecutor;
import fr.palmus.mobscontroller.scheduler.DespawnScheduler;
import fr.palmus.mobscontroller.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobsController extends JavaPlugin {

    private static MobsController INSTANCE;

    public Boolean DEBUG_MODE;

    private Logger logger;

    private DespawnScheduler despawnScheduler;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        INSTANCE = this;
        logger = new Logger();

        RegionConfig.initializeConfigFile();

        DEBUG_MODE = getConfig().getBoolean("debug");

        logger.debug(ChatColor.DARK_GREEN + "Debugger detected, additional messages will be printed in the console");

        logger.debug(ChatColor.DARK_GREEN + "-------------------------------------------------------------------");

        Plugin WE = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if (WE != null) {
            logger.debug(ChatColor.GREEN + "WorldEdit Hooked !");
        }else {
            logger.log("§cFailed to hook WorldEdit, the plugin will be unusable");
        }

        logger.debug(ChatColor.DARK_GREEN + "-------------------------------------------------------------------");

        Plugin WG = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (WG != null) {
            logger.debug(ChatColor.GREEN + "WorldGuard Hooked !");
        }else {
            logger.log("§cFailed to hook WorldGuard, the plugin will be unusable");
        }

        logger.debug(ChatColor.DARK_GREEN + "-------------------------------------------------------------------");

        // Remove all entities on server initialization
        Bukkit.getServer().getWorlds().forEach(world -> world.getEntities().forEach(Entity::remove));

        setExecutor();
        setListeners();

        logger.log("MobsController enabled successfully, all dependencies were met and hooked");

        new Scheduler();
        despawnScheduler = new DespawnScheduler();
        despawnScheduler.scheduleDespawn();
    }

    @Override
    public void onDisable() {
        saveDefaultConfig();
    }

    private void setExecutor(){
        getCommand("mobsc").setExecutor(new MobscExecutor());
        getCommand("mobsc").setTabCompleter(new MobscCompleter());
    }

    private void setListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new DamageManager(), this);
    }

    public static MobsController getInstance() {
        return INSTANCE;
    }

    public Logger getCustomLogger() {
        return logger;
    }

    public DespawnScheduler getDespawnScheduler() {
        return despawnScheduler;
    }
}