package fr.palmus.mobscontroller.config;

import fr.palmus.mobscontroller.MobsController;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class RegionConfig {

    private static final MobsController main = MobsController.getInstance();

    private static final File REGION_FILE = new File("plugins/MobsController", "region.yml");

    private static FileConfiguration regionConfiguration;

    public static void initializeConfigFile() {
        regionConfiguration = YamlConfiguration.loadConfiguration(REGION_FILE);
    }

    public static File getRegionFile() {
        return REGION_FILE;
    }

    public static FileConfiguration getRegionConfiguration() {
        return regionConfiguration;
    }

    public static void saveRegionConfig() {
        try {
            RegionConfig.getRegionConfiguration().save(RegionConfig.getRegionFile());
        } catch (IOException e) {
            main.getCustomLogger().log(e.getMessage());
            main.getLogger().log(Level.SEVERE, ChatColor.RED + "Failed to save period's config, shutting down the server");
            Bukkit.shutdown();
        }
    }

    public static void clearCache() {
        REGION_FILE.delete();
    }
}
