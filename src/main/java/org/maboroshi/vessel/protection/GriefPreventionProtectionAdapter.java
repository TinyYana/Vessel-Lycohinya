package org.maboroshi.vessel.protection;

import java.util.Locale;
import java.util.function.Supplier;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.maboroshi.vessel.util.Log;

/**
 * GriefPrevention integration, verified against the exact jar Lycohinya deploys
 * (com.griefprevention:GriefPrevention:16.18.2-SNAPSHOT via javap on the cached dependency, cross-
 * checked against upstream source at github.com/GriefPrevention/GriefPrevention).
 *
 * <p>{@code Claim#checkPermission(Player, ClaimPermission, Event)} already composes owner, explicit
 * trust, public trust, subdivision-inherits-parent-trust, Admin Claim, and {@code ignoreclaims}-mode
 * bypass — this adapter does not re-implement any of that, it only maps Vessel's config-facing
 * {@link ClaimAction} onto GriefPrevention's {@link ClaimPermission} and surfaces its denial reason.
 *
 * <p><b>Version-drift trap:</b> this pinned version's {@code ClaimPermission} enum still calls the
 * container-access tier {@code Inventory}. Upstream (18.0.0+) renamed it to {@code Container} and
 * deprecated {@code Inventory}. If the pinned GriefPrevention dependency is ever bumped to 18.0.0+,
 * update {@link #toClaimPermission} — do not assume the enum constant name is stable across GP
 * versions the way {@code ClaimAction} (Vessel's own config-facing name) is meant to be.
 */
public final class GriefPreventionProtectionAdapter implements ProtectionAdapter {
    // Package-private (not private) so its config-string parsing and GP-enum mapping can be
    // exercised directly by unit tests without needing a live GriefPrevention plugin instance.
    enum ClaimAction {
        ACCESS,
        CONTAINER,
        BUILD
    }

    private final ClaimPermission capturePermission;
    private final ClaimPermission releasePermission;

    public GriefPreventionProtectionAdapter(String captureSetting, String releaseSetting) {
        this.capturePermission = toClaimPermission(parseAction(captureSetting, ClaimAction.CONTAINER));
        this.releasePermission = toClaimPermission(parseAction(releaseSetting, ClaimAction.BUILD));
    }

    @Override
    public ProtectionResult canCapture(Player player, Location location) {
        return check(player, location, capturePermission);
    }

    @Override
    public ProtectionResult canRelease(Player player, Location location) {
        return check(player, location, releasePermission);
    }

    @Override
    public String getName() {
        return "GriefPrevention";
    }

    private ProtectionResult check(Player player, Location location, ClaimPermission permission) {
        if (player == null || location == null || location.getWorld() == null) {
            return ProtectionResult.denied(null);
        }

        DataStore dataStore = GriefPrevention.instance.dataStore;
        Claim claim = dataStore.getClaimAt(location, false, null);
        if (claim == null) {
            return ProtectionResult.ALLOWED;
        }

        Supplier<String> denial = claim.checkPermission(player, permission, null);
        return denial == null ? ProtectionResult.ALLOWED : ProtectionResult.denied(denial.get());
    }

    static ClaimAction parseAction(String configured, ClaimAction fallback) {
        if (configured == null) return fallback;
        try {
            return ClaimAction.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Log.warn("Invalid GriefPrevention permission mapping '" + configured + "', falling back to "
                    + fallback.name() + ".");
            return fallback;
        }
    }

    static ClaimPermission toClaimPermission(ClaimAction action) {
        return switch (action) {
            case ACCESS -> ClaimPermission.Access;
            case CONTAINER -> ClaimPermission.Inventory;
            case BUILD -> ClaimPermission.Build;
        };
    }
}
