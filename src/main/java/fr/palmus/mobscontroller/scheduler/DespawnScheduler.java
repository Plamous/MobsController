package fr.palmus.mobscontroller.scheduler;

import fr.palmus.mobscontroller.MobsController;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;

public class DespawnScheduler {
    private ConcurrentHashMap<Entity, Integer> entitiesToDespawn = new ConcurrentHashMap<>();
    private final MobsController main;

    public DespawnScheduler() {
        main = MobsController.getInstance();
        if (main == null) {
            throw new IllegalStateException("MobsController instance is not available.");
        }
        scheduleDespawn();
    }

    public void scheduleDespawn() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processDespawn();
            }
        }.runTaskTimer(MobsController.getInstance(), getDespawnDelay(), getDespawnDelay());
    }

    private void processDespawn() {
        for (Entity entity : entitiesToDespawn.keySet()) {
            processEntityDespawn(entity);
        }
    }

    private void processEntityDespawn(Entity entity) {
        if (entity != null && entitiesToDespawn.containsKey(entity) && !entity.isDead()) {
            if (entitiesToDespawn.get(entity) == 1) {
                entitiesToDespawn.remove(entity);
                entity.remove();
            } else {
                entitiesToDespawn.replace(entity, entitiesToDespawn.get(entity) - 1);
            }
        }
    }

    private long getDespawnDelay() {
        Long despawnDelay = main.getConfig().getLong("despawn_every") / 2 * 20L;

        return despawnDelay;
    }

    public void despawnQueue(Entity entity) {
        if (entity != null) {
            entitiesToDespawn.put(entity, 2);
        }
    }

    public void resetDespawn(Entity entity) {
        if (entity != null) {
            entitiesToDespawn.replace(entity, 2);
        }
    }

    public ConcurrentHashMap<Entity, Integer> getEntitiesToDespawn() {
        return entitiesToDespawn;
    }
}
