package org.maboroshi.vessel.storage;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

final class ChecksumUtil {
    private ChecksumUtil() {}

    static long compute(String payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload.getBytes(StandardCharsets.UTF_8));
        return crc32.getValue();
    }
}
