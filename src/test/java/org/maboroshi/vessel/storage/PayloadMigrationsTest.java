package org.maboroshi.vessel.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PayloadMigrationsTest {
    private static VesselPayload payloadAt(int schemaVersion) {
        return new VesselPayload(
                schemaVersion, 4189, "paper_entity_snapshot_v1", "COW", "{}", ChecksumUtil.compute("{}"), "v-1");
    }

    @Test
    void currentSchemaPayloadPassesThroughUnchanged() {
        VesselPayload current = payloadAt(EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION);

        assertDoesNotThrowAndReturnsSame(current);
    }

    private void assertDoesNotThrowAndReturnsSame(VesselPayload current) {
        VesselPayload result;
        try {
            result = PayloadMigrations.migrateToCurrent(current);
        } catch (VesselDataException e) {
            throw new AssertionError(e);
        }
        assertSame(current, result, "a payload already at the current schema must not be replaced by the chain");
    }

    @Test
    void unregisteredSourceVersionThrowsUnknownSchema() {
        // Schema v1 is the only version that has ever existed, so no migrator is registered below it.
        // This simulates the case a future intermediate version's migrator is missing/misregistered.
        VesselPayload fromAHypotheticalOlderSchema = payloadAt(-1);

        VesselDataException e = assertThrows(
                VesselDataException.class, () -> PayloadMigrations.migrateToCurrent(fromAHypotheticalOlderSchema));
        assertEquals(VesselDataException.Reason.UNKNOWN_SCHEMA, e.getReason());
    }
}
