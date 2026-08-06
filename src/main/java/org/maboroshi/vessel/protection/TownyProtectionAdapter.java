package org.maboroshi.vessel.protection;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class TownyProtectionAdapter implements ProtectionAdapter {

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
        return "Towny";
    }

    private ProtectionResult canBuild(Player player, Location location) {
        if (player == null || location == null) {
            return ProtectionResult.denied(null);
        }

        World world = location.getWorld();
        if (world == null) {
            return ProtectionResult.denied(null);
        }

        TownyAPI townyApi = TownyAPI.getInstance();
        if (townyApi == null || !townyApi.isTownyWorld(world)) {
            return ProtectionResult.ALLOWED;
        }

        boolean allowed = PlayerCacheUtil.getCachePermission(
                player, location, location.getBlock().getType(), ActionType.BUILD);
        return allowed ? ProtectionResult.ALLOWED : ProtectionResult.denied(null);
    }
}
