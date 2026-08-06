package org.maboroshi.vessel.storage;

/**
 * The versioned envelope around a captured entity's serialized state.
 *
 * @param schemaVersion Vessel's own payload schema version (not the Minecraft data version).
 * @param dataVersion Minecraft DataVersion at capture time, or {@link #UNKNOWN_DATA_VERSION} for
 *     payloads migrated from legacy v0 storage, which never recorded one.
 * @param codecId identifies which serializer produced {@code payload}, e.g. {@link
 *     EntitySnapshotAdapter#CODEC_ID}.
 * @param entityType {@link org.bukkit.entity.EntityType#name()} of the captured entity.
 * @param payload the codec-specific serialized entity data.
 * @param checksum CRC32 of {@code payload}'s UTF-8 bytes, guarding against truncation/corruption.
 * @param vesselId stable identifier for this specific captured instance, used for in-flight guards.
 */
public record VesselPayload(
        int schemaVersion,
        int dataVersion,
        String codecId,
        String entityType,
        String payload,
        long checksum,
        String vesselId) {
    public static final int UNKNOWN_DATA_VERSION = -1;
}
