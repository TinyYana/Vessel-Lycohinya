package org.maboroshi.vessel.storage.nbt;

/**
 * Supported NBT element types per the Minecraft NBT specification.
 */
public enum NbtType {
    END(0),
    BYTE(1),
    SHORT(2),
    INT(3),
    LONG(4),
    FLOAT(5),
    DOUBLE(6),
    BYTE_ARRAY(7),
    STRING(8),
    LIST(9),
    COMPOUND(10),
    INT_ARRAY(11),
    LONG_ARRAY(12);

    private final byte id;

    NbtType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }
}
