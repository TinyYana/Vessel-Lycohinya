package org.maboroshi.vessel.handler;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.maboroshi.vessel.Vessel;
import org.maboroshi.vessel.config.objects.CommandAction;

public class ActionHandler {
    private final Vessel plugin;
    private final boolean hasPAPI;

    public ActionHandler(Vessel plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void process(OfflinePlayer player, Collection<CommandAction> commands) {
        process(player, commands, str -> str);
    }

    public void process(
            OfflinePlayer player, Collection<CommandAction> commands, Function<String, String> commandParser) {
        if (commands == null || commands.isEmpty()) return;

        for (CommandAction action : commands) {
            if (!action.global && action.permission != null && !action.permission.isEmpty()) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null && !onlinePlayer.hasPermission(action.permission)) {
                    continue;
                }
            }

            if (action.chance < 100.0) {
                double roll = ThreadLocalRandom.current().nextDouble(100.0);
                if (roll > action.chance) continue;
            }

            if (action.global) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    executeAction(onlinePlayer, action, commandParser);
                }
            } else {
                executeAction(player, action, commandParser);
            }

            if (action.stopProcessing) {
                break;
            }
        }
    }

    private void executeAction(OfflinePlayer target, CommandAction action, Function<String, String> commandParser) {
        if (action.commands == null || action.commands.isEmpty()) return;
        if (action.pickOneRandom) {
            int index = ThreadLocalRandom.current().nextInt(action.commands.size());
            String randomCmd = action.commands.get(index);
            dispatch(target, commandParser.apply(randomCmd));
        } else {
            for (String cmd : action.commands) {
                dispatch(target, commandParser.apply(cmd));
            }
        }
    }

    private void dispatch(OfflinePlayer player, String command) {
        if (command == null || command.isEmpty()) return;

        Player online = player != null ? player.getPlayer() : null;
        if (online != null) {
            // Folia: PlaceholderAPI expansions can read this player's live entity/world state (the
            // same class of bug that broke IC's chat hover under Folia via %player_biome% — see
            // Lycohinya's docs/PLUGIN_STACK.md "D-48"), so placeholder resolution must happen on the
            // player's own thread, not whichever thread is fanning this action out to everyone online.
            online.getScheduler().run(plugin, task -> resolveAndDispatch(player, command), null);
        } else {
            resolveAndDispatch(player, command);
        }
    }

    private void resolveAndDispatch(OfflinePlayer player, String command) {
        String parsed = command;
        if (player != null) {
            String name = player.getName();
            parsed = parsed.replace("<player>", name != null ? name : "Unknown")
                    .replace("<uuid>", player.getUniqueId().toString());
        }

        if (player != null && hasPAPI) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        if (parsed.startsWith("/")) {
            parsed = parsed.substring(1);
        }

        final String finalCommand = parsed;
        Bukkit.getGlobalRegionScheduler()
                .execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}
