package org.maboroshi.vessel.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WorldGuardProtectionAdapter implements ProtectionAdapter {

    private final RegionQuery cachedQuery;

    public WorldGuardProtectionAdapter() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.cachedQuery = container.createQuery();
    }

    @Override
    public ProtectionResult canCapture(Player player, Location location) {
        return canBuild(player, location);
    }

    @Override
    public ProtectionResult canRelease(Player player, Location location) {
        return canBuild(player, location);
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }

    private ProtectionResult canBuild(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) {
            return ProtectionResult.denied(null);
        }

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        World adaptedWorld = BukkitAdapter.adapt(location.getWorld());
        com.sk89q.worldedit.util.Location adaptedLocation = BukkitAdapter.adapt(location);

        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, adaptedWorld)) {
            return ProtectionResult.ALLOWED;
        }

        return cachedQuery.testState(adaptedLocation, localPlayer, Flags.BUILD)
                ? ProtectionResult.ALLOWED
                : ProtectionResult.denied(null);
    }
}
