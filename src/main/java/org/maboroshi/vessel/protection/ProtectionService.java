package org.maboroshi.vessel.protection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.maboroshi.vessel.Vessel;

public final class ProtectionService {
    private final List<ProtectionAdapter> adapters;

    private ProtectionService(List<ProtectionAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public static ProtectionService create(Vessel plugin) {
        List<ProtectionAdapter> adapters = new ArrayList<>();

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                adapters.add(new WorldGuardProtectionAdapter());
            } catch (Throwable exception) {
                plugin.getLogger()
                        .log(
                                Level.WARNING,
                                "WorldGuard found, but the adapter failed to load. Is the version unsupported?",
                                exception);
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            try {
                adapters.add(new TownyProtectionAdapter());
            } catch (Throwable exception) {
                plugin.getLogger().log(Level.WARNING, "Towny found, but the adapter failed to load.", exception);
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            try {
                var gpSettings = plugin.getConfigManager().getMainConfig().griefprevention;
                adapters.add(new GriefPreventionProtectionAdapter(
                        gpSettings.capturePermission, gpSettings.releasePermission));
            } catch (Throwable exception) {
                plugin.getLogger()
                        .log(
                                Level.WARNING,
                                "GriefPrevention found, but the adapter failed to load. Is the version unsupported?",
                                exception);
            }
        }

        return new ProtectionService(adapters);
    }

    public ProtectionResult canCapture(Player player, Location location) {
        return canPerformCheck(player, location, true);
    }

    public ProtectionResult canRelease(Player player, Location location) {
        return canPerformCheck(player, location, false);
    }

    private ProtectionResult canPerformCheck(Player player, Location location, boolean capture) {
        for (ProtectionAdapter adapter : adapters) {
            ProtectionResult result =
                    capture ? adapter.canCapture(player, location) : adapter.canRelease(player, location);
            if (!result.allowed()) {
                return result;
            }
        }

        return ProtectionResult.ALLOWED;
    }
}
