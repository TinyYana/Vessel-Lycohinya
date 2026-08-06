package org.maboroshi.vessel.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import org.junit.jupiter.api.Test;
import org.maboroshi.vessel.protection.GriefPreventionProtectionAdapter.ClaimAction;

/**
 * Covers the config-string-to-{@link ClaimPermission} mapping in isolation. This is the part most
 * at risk of silently breaking on a GriefPrevention version bump: the pinned 16.18.2-SNAPSHOT jar
 * still names the container-access tier "Inventory" (verified via javap against the actual cached
 * dependency), while upstream 18.0.0+ renamed it to "Container" and deprecated "Inventory".
 */
class GriefPreventionProtectionAdapterTest {
    @Test
    void validValuesParseCaseInsensitively() {
        assertEquals(ClaimAction.ACCESS, GriefPreventionProtectionAdapter.parseAction("access", ClaimAction.BUILD));
        assertEquals(
                ClaimAction.CONTAINER, GriefPreventionProtectionAdapter.parseAction("Container", ClaimAction.BUILD));
        assertEquals(ClaimAction.BUILD, GriefPreventionProtectionAdapter.parseAction("BUILD", ClaimAction.ACCESS));
    }

    @Test
    void invalidValueFallsBackToTheProvidedDefault() {
        assertEquals(
                ClaimAction.BUILD, GriefPreventionProtectionAdapter.parseAction("not-a-real-value", ClaimAction.BUILD));
        assertEquals(ClaimAction.ACCESS, GriefPreventionProtectionAdapter.parseAction("", ClaimAction.ACCESS));
    }

    @Test
    void nullValueFallsBackToTheProvidedDefault() {
        assertEquals(ClaimAction.CONTAINER, GriefPreventionProtectionAdapter.parseAction(null, ClaimAction.CONTAINER));
    }

    @Test
    void claimActionMapsToTheCorrectGriefPreventionPermission() {
        assertEquals(ClaimPermission.Access, GriefPreventionProtectionAdapter.toClaimPermission(ClaimAction.ACCESS));
        assertEquals(ClaimPermission.Build, GriefPreventionProtectionAdapter.toClaimPermission(ClaimAction.BUILD));
        // Version-drift trap: pinned GriefPrevention still calls this tier "Inventory", not "Container".
        assertEquals(
                ClaimPermission.Inventory, GriefPreventionProtectionAdapter.toClaimPermission(ClaimAction.CONTAINER));
    }
}
