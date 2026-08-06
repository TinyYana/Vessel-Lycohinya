package org.maboroshi.vessel.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Vanilla NBT strings hard-cap at 65,535 modified-UTF-8 bytes and silently save as empty past that
 * limit (DataOutput#writeUTF) — validateSize must reject before that ever reaches a save.
 */
class EntitySnapshotAdapterSizeTest {
    @Test
    void smallPayloadIsAccepted() {
        assertDoesNotThrow(() -> EntitySnapshotAdapter.validateSize("{Type:\"minecraft:cow\"}", "COW"));
    }

    @Test
    void payloadPastTheSafeThresholdIsRejected() {
        String oversized = "x".repeat(60_001);
        VesselDataException e = assertThrows(
                VesselDataException.class, () -> EntitySnapshotAdapter.validateSize(oversized, "VILLAGER"));
        assertEquals(VesselDataException.Reason.TOO_LARGE, e.getReason());
    }

    @Test
    void payloadAtExactlyTheThresholdIsAccepted() {
        String exact = "x".repeat(60_000);
        assertDoesNotThrow(() -> EntitySnapshotAdapter.validateSize(exact, "VILLAGER"));
    }
}
