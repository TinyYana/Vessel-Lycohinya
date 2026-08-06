package org.maboroshi.vessel.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionAdapter {
    ProtectionResult canCapture(Player player, Location location);

    ProtectionResult canRelease(Player player, Location location);

    String getName();
}
