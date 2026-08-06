package org.maboroshi.vessel.storage;

import java.util.UUID;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.vessel.storage.VesselDataException.Reason;
import org.maboroshi.vessel.util.Keys;

/**
 * Reads/writes a {@link VesselPayload} to/from an item's {@link PersistentDataContainer}, handling
 * legacy v0 detection and lazy migration to the current schema. This is the only class Listeners
 * should touch for entity data persistence — it never leaves a container partially written, and it
 * never throws for a container that simply has no vessel data (callers check that themselves via
 * {@link org.maboroshi.vessel.util.VesselUtils#getTemplateId}).
 */
public final class VesselPayloadStore {
    private VesselPayloadStore() {}

    /** @param migrated true if the container's data was in an older schema than current. */
    public record ReadResult(VesselPayload payload, EntitySnapshot snapshot, boolean migrated) {}

    /**
     * Reads and, if necessary, migrates the payload to the current schema. Never mutates {@code
     * pdc} — callers decide whether/when to persist a migrated payload back via {@link #write}.
     *
     * @throws VesselDataException if the payload is missing, corrupt, from an unknown future schema,
     *     or fails its checksum. The original item must be left untouched when this is thrown.
     */
    public static ReadResult readAndMigrate(PersistentDataContainer pdc) throws VesselDataException {
        Integer storedSchemaVersion = pdc.get(Keys.SCHEMA_VERSION, PersistentDataType.INTEGER);
        boolean migrated =
                storedSchemaVersion == null || storedSchemaVersion < EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION;

        VesselPayload current = PayloadMigrations.migrateToCurrent(read(pdc));
        EntitySnapshotAdapter.validateSize(current.payload(), current.entityType());
        EntitySnapshot snapshot = EntitySnapshotAdapter.toSnapshot(current);
        return new ReadResult(current, snapshot, migrated);
    }

    /** Persists a current-schema payload's full envelope onto {@code pdc}. */
    public static void write(PersistentDataContainer pdc, VesselPayload payload) throws VesselDataException {
        EntitySnapshotAdapter.validateSize(payload.payload(), payload.entityType());
        pdc.set(Keys.MOB_DATA, PersistentDataType.STRING, payload.payload());
        pdc.set(Keys.SCHEMA_VERSION, PersistentDataType.INTEGER, payload.schemaVersion());
        pdc.set(Keys.DATA_VERSION, PersistentDataType.INTEGER, payload.dataVersion());
        pdc.set(Keys.CODEC_ID, PersistentDataType.STRING, payload.codecId());
        pdc.set(Keys.ENTITY_TYPE, PersistentDataType.STRING, payload.entityType());
        pdc.set(Keys.CHECKSUM, PersistentDataType.LONG, payload.checksum());
        pdc.set(Keys.VESSEL_ID, PersistentDataType.STRING, payload.vesselId());
    }

    private static VesselPayload read(PersistentDataContainer pdc) throws VesselDataException {
        String rawPayload = pdc.get(Keys.MOB_DATA, PersistentDataType.STRING);
        if (rawPayload == null || rawPayload.isEmpty()) {
            throw new VesselDataException(Reason.MALFORMED, "No entity data stored on this item");
        }

        Integer schemaVersion = pdc.get(Keys.SCHEMA_VERSION, PersistentDataType.INTEGER);
        if (schemaVersion == null) {
            return wrapLegacyV0(pdc, rawPayload);
        }

        String codecId = pdc.getOrDefault(Keys.CODEC_ID, PersistentDataType.STRING, "");
        String entityType = pdc.getOrDefault(Keys.ENTITY_TYPE, PersistentDataType.STRING, "");
        Integer dataVersion = pdc.get(Keys.DATA_VERSION, PersistentDataType.INTEGER);
        Long checksum = pdc.get(Keys.CHECKSUM, PersistentDataType.LONG);
        String vesselId = pdc.getOrDefault(Keys.VESSEL_ID, PersistentDataType.STRING, "");

        return decodeEnvelope(schemaVersion, dataVersion, codecId, entityType, rawPayload, checksum, vesselId);
    }

    /**
     * Pure envelope-field validation, deliberately free of any {@link PersistentDataContainer}
     * dependency so it can be exercised directly by unit tests without a live Bukkit server.
     */
    static VesselPayload decodeEnvelope(
            int schemaVersion,
            Integer dataVersion,
            String codecId,
            String entityType,
            String rawPayload,
            Long checksum,
            String vesselId)
            throws VesselDataException {
        if (schemaVersion > EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION) {
            throw new VesselDataException(
                    Reason.UNKNOWN_SCHEMA,
                    "Payload schema version " + schemaVersion + " is newer than this build of Vessel supports");
        }

        if (dataVersion == null
                || checksum == null
                || codecId == null
                || codecId.isEmpty()
                || entityType == null
                || entityType.isEmpty()) {
            throw new VesselDataException(Reason.MALFORMED, "Versioned payload envelope is incomplete");
        }

        long actualChecksum = ChecksumUtil.compute(rawPayload);
        if (actualChecksum != checksum) {
            throw new VesselDataException(
                    Reason.CHECKSUM_MISMATCH,
                    "Stored checksum " + checksum + " does not match computed checksum " + actualChecksum);
        }

        return new VesselPayload(schemaVersion, dataVersion, codecId, entityType, rawPayload, checksum, vesselId);
    }

    /** Wraps a legacy v0 (pre-envelope) raw {@code EntitySnapshot#getAsString()} value as schema v1. */
    private static VesselPayload wrapLegacyV0(PersistentDataContainer pdc, String rawPayload)
            throws VesselDataException {
        EntitySnapshot legacySnapshot = EntitySnapshotAdapter.legacySnapshot(rawPayload);

        String vesselId = pdc.get(Keys.VESSEL_ID, PersistentDataType.STRING);
        if (vesselId == null || vesselId.isEmpty()) {
            vesselId = UUID.randomUUID().toString();
        }

        return new VesselPayload(
                EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                VesselPayload.UNKNOWN_DATA_VERSION,
                EntitySnapshotAdapter.CODEC_ID,
                legacySnapshot.getEntityType().name(),
                rawPayload,
                ChecksumUtil.compute(rawPayload),
                vesselId);
    }
}
