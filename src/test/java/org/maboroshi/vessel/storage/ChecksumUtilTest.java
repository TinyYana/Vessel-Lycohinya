package org.maboroshi.vessel.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ChecksumUtilTest {
    @Test
    void sameInputProducesSameChecksum() {
        assertEquals(
                ChecksumUtil.compute("{Type:\"minecraft:cow\"}"), ChecksumUtil.compute("{Type:\"minecraft:cow\"}"));
    }

    @Test
    void differentInputProducesDifferentChecksum() {
        assertNotEquals(
                ChecksumUtil.compute("{Type:\"minecraft:cow\"}"), ChecksumUtil.compute("{Type:\"minecraft:pig\"}"));
    }

    @Test
    void detectsSingleCharacterCorruption() {
        String original = "{Health:10.0f,Type:\"minecraft:wolf\"}";
        String corrupted = "{Health:11.0f,Type:\"minecraft:wolf\"}";
        assertNotEquals(ChecksumUtil.compute(original), ChecksumUtil.compute(corrupted));
    }

    @Test
    void emptyStringHasAStableChecksum() {
        assertEquals(ChecksumUtil.compute(""), ChecksumUtil.compute(""));
    }
}
