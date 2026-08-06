package org.maboroshi.vessel.storage;

/**
 * A captured entity's stored data could not be safely used. Callers must reject the operation and
 * leave the original item untouched — never consume a vessel or spawn an entity on this path.
 */
public class VesselDataException extends Exception {
    public enum Reason {
        /** The payload is larger than can be safely persisted (see {@link EntitySnapshotAdapter}). */
        TOO_LARGE,
        /** {@code schema-version} is higher than this build of Vessel understands. */
        UNKNOWN_SCHEMA,
        /** The stored checksum does not match the stored payload. */
        CHECKSUM_MISMATCH,
        /** The payload could not be parsed by its codec, or required fields were missing/malformed. */
        MALFORMED
    }

    private final Reason reason;

    public VesselDataException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public VesselDataException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
