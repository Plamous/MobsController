package fr.palmus.mobscontroller.scheduler;

import com.mohistmc.api.EntityAPI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import fr.palmus.mobscontroller.MobsController;
import fr.palmus.mobscontroller.config.RegionConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Scheduler class is responsible for spawning entities in specific regions at regular intervals.
 */
public class Scheduler {

    private final MobsController main;

    public Scheduler() {
        main = MobsController.getInstance();
        startThread();
    }

    /**
     * MAY/SHOULD BE OPTIMIZED OR AT LEAST REFACTOREDD
     *
     * Starts a new thread to handle spawning of entities according to the configured regions and mob types.
     * This method uses CompletableFuture to run the spawning phase asynchronously.
     *
     * <p>The spawning phase consists of the following steps:</p>
     * <ol>
     *   <li>Retrieve the configured regions from the region.yml file.</li>
     *   <li>Loop through each region and retrieve the associated mob names and their spawn probability.</li>
     *   <li>Based on the probability, add the mob name to the spawn list for the region.</li>
     *   <li>Once all regions have been processed, spawn the entities for each region in a new thread.</li>
     *   <li>After spawning, remove the processed regions from the map.</li>
     *   <li>Log the completion of the spawning phase.</li>
     * </ol>
     *
     * <p>The spawning interval and the world name are retrieved from the main configuration.</p>
     */
    private void startThread() {
        new BukkitRunnable() {
            @Override
            public void run() {
                CompletableFuture<Map<ProtectedRegion, List<String>>> future = CompletableFuture.supplyAsync(() -> {
                    main.getCustomLogger().debug("Starting a spawning phase...");
                    Map<ProtectedRegion, List<String>> toSpawn = new HashMap<>();
                    FileConfiguration config = RegionConfig.getRegionConfiguration();

                    World world = Bukkit.getWorld(main.getConfig().getString("world_name"));
                    if (world == null) {
                        main.getCustomLogger().debug("Unknown world -> " + main.getConfig().getString("world_name"));
                        return null;
                    }

                    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                    RegionManager regions = container.get(BukkitAdapter.adapt(world));

                    for (String regionName : config.getKeys(false)) {
                        ProtectedRegion region = regions.getRegion(regionName);
                        if (region == null) {
                            config.set(regionName, null);
                            main.getCustomLogger().debug("Found a deleted region in the config, data has been cleared");
                            break;
                        }

                        if(!toSpawn.containsKey(regionName)) {
                            toSpawn.put(region, new ArrayList<>());
                        }

                        List<String> list = toSpawn.get(region);

                        for (String mobName : config.getConfigurationSection(regionName).getKeys(false)) {
                            int chance = config.getInt(regionName + "." + mobName + ".chance");

                            if (chance == 0 || ThreadLocalRandom.current().nextInt(101) <= chance) {
                                list.add(mobName);
                            }
                        }
                        toSpawn.replace(region, list);
                    }
                    return toSpawn;
                });

                future.thenAccept(value -> {
                    if (value == null) {
                        return;
                    }

                    for (ProtectedRegion reg : value.keySet()) {
                        try {
                            spawnEntities(reg, value.get(reg));
                        } catch (Exception e) {
                            main.getCustomLogger().log("Error during spawning entities for region: " + reg.getId());
                            e.printStackTrace();
                        }
                    }

                    main.getCustomLogger().debug("Spawning phase done");
                });
            }
        }.runTaskTimer(main, main.getConfig().getInt("spawn_every") * 20L, main.getConfig().getInt("spawn_every") * 20L);
    }

    /**
     * MAY/SHOULD BE OPTIMIZED OR AT LEAST REFACTORED
     *
     * Spawns entities within a given protected region.
     *
     * @param reg       the protected region where entities will be spawned
     * @param mobNames  a list of mob names to be spawned within the region
     */
    private void spawnEntities(ProtectedRegion reg, List<String> mobNames) {
        World world = Bukkit.getWorld(main.getConfig().getString("world_name"));
        Location center = calculateRegionCenter(reg, world);

        for (String mobName : mobNames) {
            EntityType entityType = EntityAPI.entityType(mobName);

            if (entityType == EntityType.UNKNOWN) {
                this.main.getCustomLogger().debug("Invalid EntityType for " + mobName);
                return;
            }

            if (RegionConfig.getRegionConfiguration().getString(reg.getId() + "." + mobName + ".name") == null) {
                this.main.getCustomLogger().debug("Invalid Configuration for " + mobName + " the name for this mob hasn't been defined");
                return;
            }

            int count = 0;
            for (Entity ent : world.getNearbyEntities(center, center.getX(), 50, center.getX())) {
                if (ent.getName().equalsIgnoreCase(RegionConfig.getRegionConfiguration().getString(reg.getId() + "." + mobName + ".name").replace('_', ' ')))
                    count++;
            }

            int cap = RegionConfig.getRegionConfiguration().getInt(reg.getId() + "." + mobName + ".cap");
            for(int i = RegionConfig.getRegionConfiguration().getInt(reg.getId() + "." + mobName + ".number"); i > 0; i--) {
                if (count < cap) {
                    Location spawnLoc = getValidLocation(world, reg);
                    if(spawnLoc == null) {
                        return;
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try{
                                Entity spawnedEntity = world.spawnEntity(spawnLoc, entityType);
                                main.getDespawnScheduler().despawnQueue(spawnedEntity);
                            }catch (IllegalArgumentException e) {
                                String formattedName = mobName.toLowerCase().replace("extreme_", "extreme:");
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon " + formattedName + " " + spawnLoc.getBlockX() + " " + spawnLoc.getBlockY() + " " + spawnLoc.getBlockZ());
                                for(Entity ent : world.getNearbyEntities(new Location(world, spawnLoc.getBlockX(), spawnLoc.getBlockY(), spawnLoc.getBlockZ()), 2, 2, 2)) {
                                    main.getDespawnScheduler().despawnQueue(ent);
                                }
                            }
                        }
                    }.runTask(main);
                    count++;
                }
            }
        }
    }

    private Location calculateRegionCenter(ProtectedRegion reg, World world) {
        double centerX = (reg.getMinimumPoint().getX() + (reg.getMaximumPoint().getX() - reg.getMinimumPoint().getX()) / 2);
        double centerZ = (reg.getMinimumPoint().getZ() + (reg.getMaximumPoint().getZ() - reg.getMinimumPoint().getZ()) / 2);
        double centerY = world.getHighestBlockYAt((int) centerX, (int) centerZ) + 1;

        return new Location(world, centerX, centerY, centerZ);
    }

    private Location getValidLocation(World world, ProtectedRegion reg) {
        int maxAttempt = main.getConfig().getInt("max_attempt");
        int spawnDistanceThreshold = main.getConfig().getInt("spawn_distance_threshold");
        int inside = main.getConfig().getInt("chance_to_spawn_inside");

        for(int i = 0; i < maxAttempt; i++){
            int x = ThreadLocalRandom.current().nextInt(reg.getMinimumPoint().getX(), reg.getMaximumPoint().getX() + 1);
            int z = ThreadLocalRandom.current().nextInt(reg.getMinimumPoint().getZ(), reg.getMaximumPoint().getZ() + 1);

            int highestY = world.getHighestBlockYAt(x, z);
            int y = highestY;

            for(int j = inside; j > 0; j--) {
                y = ThreadLocalRandom.current().nextInt(10, highestY);

                if(world.getBlockAt(x, y, z).getType() == Material.AIR) {
                    break;
                }
            }

            if(world.getBlockAt(x, y, z).getType() != Material.AIR) {
                y = highestY + 1;
            }

            boolean isLocationValid = true;
            for(Player pls : Bukkit.getOnlinePlayers()) {
                Location playerLocation = pls.getLocation();

                //check if the candidate location is within the spawn distance threshold
                if(playerLocation.distanceSquared(new Location(world, x, y, z)) <= spawnDistanceThreshold * spawnDistanceThreshold) {
                    isLocationValid = false;
                    break;
                }
            }

            //only create a new Location object if we need to return it
            if(isLocationValid) {
                return new Location(world, x, y, z);
            }
        }

        return null;
    }
}
