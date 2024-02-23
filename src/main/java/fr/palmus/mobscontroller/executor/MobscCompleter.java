package fr.palmus.mobscontroller.executor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MobscCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if(!(sender instanceof Player)) {
            return null;
        }

        Player pl = (Player) sender;

        final List<String> completions = new ArrayList<>();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(pl.getWorld()));

        if(args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("clear", "add", "remove", "set-cap", "set-chance", "set-name", "set-number"), completions);
        }

        if(args.length == 2) {
            if(regions == null) {
                return null;
            }

            StringUtil.copyPartialMatches(args[1], regions.getRegions().keySet(), completions);
        }

        if(args.length == 3 && args[0] != "set-cap") {
            EntityType[] entityTypes = EntityType.values();
            List<String> entityTypeStrings = new ArrayList<>();

            for (EntityType entityType : entityTypes) {
                entityTypeStrings.add(entityType.toString());
            }

            StringUtil.copyPartialMatches(args[2], entityTypeStrings, completions);
        }

        return completions;
    }
}
