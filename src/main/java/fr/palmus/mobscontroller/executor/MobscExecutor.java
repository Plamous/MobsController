package fr.palmus.mobscontroller.executor;

import com.mohistmc.api.EntityAPI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import fr.palmus.mobscontroller.MobsController;
import fr.palmus.mobscontroller.config.RegionConfig;
import fr.palmus.mobscontroller.messages.Message;
import fr.palmus.mobscontroller.messages.PrefixLevel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MobscExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            Message.sendMessage(sender, PrefixLevel.ERROR, "Seul un joueur peut exécuter cette commande");
                return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(Message.BAD_USAGE);
            return true;
        }

        String regionName = args[1];

        if (!isValidRegion(player, regionName)) {
            return true;
        }

        if(args.length >= 3) {
            if (!isValidMob(player, args[2], regionName)) {
                return true;
            }
            args[2] = args[2].toUpperCase();
        }

        switch (args[0]) {
            case "clear":
                handleClearCommand(player, regionName, args);
                break;
            case "add":
                handleAddRemoveCommand(player, regionName, args, true);
                break;
            case "remove":
                handleAddRemoveCommand(player, regionName, args, false);
                break;
            case "set-chance":
            case "set-name":
            case "set-number":
            case "set-cap":
                handleSetCommand(player, regionName, args);
                break;
            default:
                debugMessage("Default state");
                player.sendMessage(Message.BAD_USAGE);
                return true;
        }
        return true;
    }

    private void handleClearCommand(Player player, String regionName, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Message.BAD_USAGE);
            return;
        }

        RegionConfig.getRegionConfiguration().set(regionName, null);
        RegionConfig.saveRegionConfig();
        Message.sendPlayerMessage(player, PrefixLevel.GOOD, "Toutes les données pour la région " + regionName + " ont été supprimées");
        debugMessage("Cleared data for region: " + regionName);
    }

    private void handleAddRemoveCommand(Player player, String regionName, String[] args, boolean isAdd) {
        if (args.length != 3) {
            player.sendMessage(Message.BAD_USAGE);
            return;
        }

        String entityName = args[2];
        String path = regionName + "." + entityName;

        if (isAdd) {
            RegionConfig.getRegionConfiguration().set(path, entityName);
            Message.sendPlayerMessage(player, PrefixLevel.GOOD, entityName + " a été ajouté en tant qu'entité spawnable pour la région " + regionName);
        } else {
            RegionConfig.getRegionConfiguration().set(path, null);
            Message.sendPlayerMessage(player, PrefixLevel.GOOD, entityName + " n'est plus une entité spawnable pour la région " + regionName);
        }

        RegionConfig.saveRegionConfig();
        debugMessage((isAdd ? "Added" : "Removed") + " entity: " + entityName + " for region: " + regionName);
    }

    private void handleSetCommand(Player player, String regionName, String[] args) {
        if (args.length != 4) {
            player.sendMessage(Message.BAD_USAGE);
            return;
        }

        String entityName = args[2];
        String setting = args[0].equals("set-chance") ? "chance" : "cap";

        if(setting.equals("cap")){
            setting = args[0].equals("set-cap") ? "cap" : "name";
        }

        if(setting.equals("name")){
            setting = args[0].equals("set-name") ? "name" : "number";
        }

        if (!configContainsMob(regionName, entityName, player)) {
            return;
        }

        if(setting.equalsIgnoreCase("name")) {
            RegionConfig.getRegionConfiguration().set(regionName + "." + entityName + "." + setting, args[3]);
            RegionConfig.saveRegionConfig();
            Message.sendPlayerMessage(player, PrefixLevel.GOOD, entityName + " a maintenant une " + setting + " de " + args[3] + " dans " + regionName);
            return;
        }

        try {
            int value = Integer.parseInt(args[3]);
            RegionConfig.getRegionConfiguration().set(regionName + "." + entityName + "." + setting, value);
            RegionConfig.saveRegionConfig();
            Message.sendPlayerMessage(player, PrefixLevel.GOOD, entityName + " a maintenant une " + setting + " de " + value + " dans " + regionName);
            debugMessage("Set " + setting + " for entity: " + entityName + " in region: " + regionName + " to: " + value);
        } catch (NumberFormatException e) {
            Message.sendMessage(player, PrefixLevel.ERROR, "La valeur spécifiée n'est pas un nombre -> " + args[3]);
        }
    }

    private boolean isValidRegion(Player player, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
        ProtectedRegion region = regions.getRegion(regionName);

        if (region == null) {
            Message.sendPlayerMessage(player, PrefixLevel.ERROR, "La région nommée: " + regionName + " n'existe pas dans votre monde actuel, \"/rg list\" pour voir toutes les régions");
            return false;
        }

        return true;
    }

    private boolean isValidMob(Player player, String mobName, String regName) {
        EntityType entityType = EntityAPI.entityType(mobName);

        if (entityType == EntityType.UNKNOWN) {
            Message.sendPlayerMessage(player, PrefixLevel.ERROR, "Le mob nommé: " + mobName + " n'existe pas sur ce serveur");
            return false;
        }

        if (!mobName.contains("ZOMBIE_EXTREME") && mobName.contains("_")) {
            Message.sendPlayerMessage(player, PrefixLevel.ERROR, "Le mob nommé: " + mobName + " n'est pas pris en charge par le serveur, merci de contacter un développeur pour ajouter de nouveaux mobs au plugin");
            return false;
        }

        if (RegionConfig.getRegionConfiguration().contains(regName + "." + mobName.toUpperCase())) {
            return true;
        }

        return true;
    }

    private boolean configContainsMob(String regionName, String entityName, Player player) {
        if (RegionConfig.getRegionConfiguration().contains(regionName + "." + entityName)) {
            return true;
        }

        Message.sendPlayerMessage(player, PrefixLevel.ERROR, "L'entité spécifiée n'est pas une entité spawnable pour la région " + regionName);
        return false;
    }

    private void debugMessage(String msg) {
        MobsController.getInstance().getCustomLogger().debug(msg);
    }
}
