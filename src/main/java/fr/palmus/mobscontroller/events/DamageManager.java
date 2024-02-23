package fr.palmus.mobscontroller.events;

import fr.palmus.mobscontroller.MobsController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageManager implements Listener {

    MobsController main = MobsController.getInstance();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && main.getDespawnScheduler().getEntitiesToDespawn().containsKey(e.getEntity())){
            main.getDespawnScheduler().resetDespawn(e.getEntity());
        }
    }
}
