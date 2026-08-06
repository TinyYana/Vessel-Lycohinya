package org.maboroshi.vessel.protection;

public record ProtectionResult(boolean allowed, String denialReason) {
    public static final ProtectionResult ALLOWED = new ProtectionResult(true, null);

    public static ProtectionResult denied(String reason) {
        return new ProtectionResult(false, reason);
    }
}
