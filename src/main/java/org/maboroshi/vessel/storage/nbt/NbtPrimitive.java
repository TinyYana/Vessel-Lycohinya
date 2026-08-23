package org.maboroshi.vessel.storage.nbt;

import java.util.Arrays;
import java.util.Objects;

/**
 * Container class for primitive and array NBT elements.
 */
public final class NbtPrimitive {
    private NbtPrimitive() {}

    public static final class NbtByte implements NbtElement {
        private final byte value;

        public NbtByte(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.BYTE;
        }

        @Override
        public NbtByte copy() {
            return new NbtByte(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtByte nbtByte)) return false;
            return value == nbtByte.value;
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtShort implements NbtElement {
        private final short value;

        public NbtShort(short value) {
            this.value = value;
        }

        public short getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.SHORT;
        }

        @Override
        public NbtShort copy() {
            return new NbtShort(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtShort nbtShort)) return false;
            return value == nbtShort.value;
        }

        @Override
        public int hashCode() {
            return Short.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtInt implements NbtElement {
        private final int value;

        public NbtInt(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.INT;
        }

        @Override
        public NbtInt copy() {
            return new NbtInt(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtInt nbtInt)) return false;
            return value == nbtInt.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtLong implements NbtElement {
        private final long value;

        public NbtLong(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.LONG;
        }

        @Override
        public NbtLong copy() {
            return new NbtLong(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtLong nbtLong)) return false;
            return value == nbtLong.value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtFloat implements NbtElement {
        private final float value;

        public NbtFloat(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.FLOAT;
        }

        @Override
        public NbtFloat copy() {
            return new NbtFloat(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtFloat nbtFloat)) return false;
            return Float.compare(nbtFloat.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtDouble implements NbtElement {
        private final double value;

        public NbtDouble(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.DOUBLE;
        }

        @Override
        public NbtDouble copy() {
            return new NbtDouble(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtDouble nbtDouble)) return false;
            return Double.compare(nbtDouble.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtString implements NbtElement {
        private final String value;

        public NbtString(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public String getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.STRING;
        }

        @Override
        public NbtString copy() {
            return new NbtString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtString nbtString)) return false;
            return value.equals(nbtString.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtByteArray implements NbtElement {
        private final byte[] value;

        public NbtByteArray(byte[] value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public byte[] getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.BYTE_ARRAY;
        }

        @Override
        public NbtByteArray copy() {
            return new NbtByteArray(Arrays.copyOf(value, value.length));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtByteArray that)) return false;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtIntArray implements NbtElement {
        private final int[] value;

        public NbtIntArray(int[] value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public int[] getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.INT_ARRAY;
        }

        @Override
        public NbtIntArray copy() {
            return new NbtIntArray(Arrays.copyOf(value, value.length));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtIntArray that)) return false;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }

    public static final class NbtLongArray implements NbtElement {
        private final long[] value;

        public NbtLongArray(long[] value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public long[] getValue() {
            return value;
        }

        @Override
        public NbtType getType() {
            return NbtType.LONG_ARRAY;
        }

        @Override
        public NbtLongArray copy() {
            return new NbtLongArray(Arrays.copyOf(value, value.length));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NbtLongArray that)) return false;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return toSnbt();
        }
    }
}
