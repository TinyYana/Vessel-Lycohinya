package org.maboroshi.vessel.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Exercises {@link VesselPayloadStore#decodeEnvelope}, the pure part of reading a versioned
 * payload back out of an item's PersistentDataContainer. The PDC-facing wrapper (`read`) and the
 * legacy-v0 path need a live Bukkit server (EntityFactory) and are covered manually — see
 * docs/TESTING.md.
 */
class VesselPayloadStoreEnvelopeTest {
    private static final String RAW_PAYLOAD = "{Health:10.0f,id:\"minecraft:cow\"}";
    private static final long VALID_CHECKSUM = ChecksumUtil.compute(RAW_PAYLOAD);

    @Test
    void validEnvelopeRoundTrips() throws VesselDataException {
        VesselPayload result = VesselPayloadStore.decodeEnvelope(
                EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                4189,
                "paper_entity_snapshot_v1",
                "COW",
                RAW_PAYLOAD,
                VALID_CHECKSUM,
                "v-1");

        assertEquals(EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION, result.schemaVersion());
        assertEquals(4189, result.dataVersion());
        assertEquals("COW", result.entityType());
        assertEquals(RAW_PAYLOAD, result.payload());
        assertEquals(VALID_CHECKSUM, result.checksum());
        assertEquals("v-1", result.vesselId());
    }

    @Test
    void checksumMismatchIsRejected() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                        4189,
                        "paper_entity_snapshot_v1",
                        "COW",
                        RAW_PAYLOAD,
                        VALID_CHECKSUM + 1,
                        "v-1"));
        assertEquals(VesselDataException.Reason.CHECKSUM_MISMATCH, e.getReason());
    }

    @Test
    void unknownFutureSchemaIsRejected() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION + 1,
                        4189,
                        "paper_entity_snapshot_v1",
                        "COW",
                        RAW_PAYLOAD,
                        VALID_CHECKSUM,
                        "v-1"));
        assertEquals(VesselDataException.Reason.UNKNOWN_SCHEMA, e.getReason());
    }

    @Test
    void missingDataVersionIsMalformed() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                        null,
                        "paper_entity_snapshot_v1",
                        "COW",
                        RAW_PAYLOAD,
                        VALID_CHECKSUM,
                        "v-1"));
        assertEquals(VesselDataException.Reason.MALFORMED, e.getReason());
    }

    @Test
    void missingChecksumIsMalformed() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                        4189,
                        "paper_entity_snapshot_v1",
                        "COW",
                        RAW_PAYLOAD,
                        null,
                        "v-1"));
        assertEquals(VesselDataException.Reason.MALFORMED, e.getReason());
    }

    @Test
    void emptyEntityTypeIsMalformed() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                        4189,
                        "paper_entity_snapshot_v1",
                        "",
                        RAW_PAYLOAD,
                        VALID_CHECKSUM,
                        "v-1"));
        assertEquals(VesselDataException.Reason.MALFORMED, e.getReason());
    }

    @Test
    void emptyCodecIdIsMalformed() {
        VesselDataException e = assertThrows(
                VesselDataException.class,
                () -> VesselPayloadStore.decodeEnvelope(
                        EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                        4189,
                        "",
                        "COW",
                        RAW_PAYLOAD,
                        VALID_CHECKSUM,
                        "v-1"));
        assertEquals(VesselDataException.Reason.MALFORMED, e.getReason());
    }

    /** Decoding the same input twice must yield the same result — migration/decoding must be idempotent. */
    @Test
    void decodingIsIdempotent() throws VesselDataException {
        VesselPayload first = VesselPayloadStore.decodeEnvelope(
                EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                4189,
                "paper_entity_snapshot_v1",
                "COW",
                RAW_PAYLOAD,
                VALID_CHECKSUM,
                "v-1");
        VesselPayload second = VesselPayloadStore.decodeEnvelope(
                EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                4189,
                "paper_entity_snapshot_v1",
                "COW",
                RAW_PAYLOAD,
                VALID_CHECKSUM,
                "v-1");
        assertEquals(first, second);
    }
}
