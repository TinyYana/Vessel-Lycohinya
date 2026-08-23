package org.maboroshi.vessel.storage.nbt;

/**
 * Base interface for all NBT elements.
 */
public interface NbtElement {
    /**
     * Gets the type of this NBT element.
     *
     * @return the NbtType
     */
    NbtType getType();

    /**
     * Creates a deep copy of this NBT element.
     *
     * @return a deep copy
     */
    NbtElement copy();

    /**
     * Serializes this NBT element to its SNBT representation.
     *
     * @return SNBT string
     */
    default String toSnbt() {
        return SnbtSerializer.serialize(this);
    }
}
