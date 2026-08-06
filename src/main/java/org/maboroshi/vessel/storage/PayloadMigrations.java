package org.maboroshi.vessel.storage;

import java.util.Map;

/**
 * Registry of forward migrators, keyed by the schema version they migrate <em>from</em>. Each
 * migrator takes a payload at version N and returns one at version N+1; {@link #migrateToCurrent}
 * chains them until the payload reaches {@link EntitySnapshotAdapter#CURRENT_SCHEMA_VERSION}.
 *
 * <p>Legacy v0 (no envelope at all) is not a {@link VesselPayload} and is handled separately in
 * {@link VesselPayloadStore} before this chain ever runs — this registry only covers v1-and-later.
 */
final class PayloadMigrations {
    // No migrators registered yet: schema v1 is the only version that has ever existed. When v2 is
    // introduced, add its migrator here, e.g. Map.of(1, new V1ToV2Migrator()), and bump
    // EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION.
    private static final Map<Integer, PayloadMigrator> MIGRATORS = Map.of();

    private PayloadMigrations() {}

    static VesselPayload migrateToCurrent(VesselPayload payload) throws VesselDataException {
        VesselPayload current = payload;
        while (current.schemaVersion() < EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION) {
            PayloadMigrator migrator = MIGRATORS.get(current.schemaVersion());
            if (migrator == null) {
                throw new VesselDataException(
                        VesselDataException.Reason.UNKNOWN_SCHEMA,
                        "No migrator registered from schema version " + current.schemaVersion());
            }
            current = migrator.migrate(current);
        }
        return current;
    }

    @FunctionalInterface
    interface PayloadMigrator {
        VesselPayload migrate(VesselPayload old) throws VesselDataException;
    }
}
