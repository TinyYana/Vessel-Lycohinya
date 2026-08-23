package org.maboroshi.vessel.storage.nbt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an NBT Compound tag (key-value mapping of String to NbtElement).
 * Preserves key insertion order for predictable serialization.
 */
public final class NbtCompound implements NbtElement {
    private final Map<String, NbtElement> entries;

    public NbtCompound() {
        this.entries = new LinkedHashMap<>();
    }

    public NbtCompound(Map<String, NbtElement> initialEntries) {
        this.entries = new LinkedHashMap<>(initialEntries);
    }

    @Override
    public NbtType getType() {
        return NbtType.COMPOUND;
    }

    public Map<String, NbtElement> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public Set<String> getKeys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    public NbtElement get(String key) {
        return entries.get(key);
    }

    public void put(String key, NbtElement element) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(element, "element");
        entries.put(key, element);
    }

    public NbtElement remove(String key) {
        return entries.remove(key);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    public NbtCompound getCompound(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtCompound compound ? compound : null;
    }

    public NbtList getList(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtList list ? list : null;
    }

    public String getString(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtString str ? str.getValue() : null;
    }

    public Byte getByte(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtByte b ? b.getValue() : null;
    }

    public Short getShort(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtShort s ? s.getValue() : null;
    }

    public Integer getInt(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtInt i ? i.getValue() : null;
    }

    public Long getLong(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtLong l ? l.getValue() : null;
    }

    public Float getFloat(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtFloat f ? f.getValue() : null;
    }

    public Double getDouble(String key) {
        NbtElement el = entries.get(key);
        return el instanceof NbtPrimitive.NbtDouble d ? d.getValue() : null;
    }

    public void putString(String key, String value) {
        put(key, new NbtPrimitive.NbtString(value));
    }

    public void putByte(String key, byte value) {
        put(key, new NbtPrimitive.NbtByte(value));
    }

    public void putShort(String key, short value) {
        put(key, new NbtPrimitive.NbtShort(value));
    }

    public void putInt(String key, int value) {
        put(key, new NbtPrimitive.NbtInt(value));
    }

    public void putLong(String key, long value) {
        put(key, new NbtPrimitive.NbtLong(value));
    }

    public void putFloat(String key, float value) {
        put(key, new NbtPrimitive.NbtFloat(value));
    }

    public void putDouble(String key, double value) {
        put(key, new NbtPrimitive.NbtDouble(value));
    }

    public void putBoolean(String key, boolean value) {
        putByte(key, (byte) (value ? 1 : 0));
    }

    @Override
    public NbtCompound copy() {
        NbtCompound copy = new NbtCompound();
        for (Map.Entry<String, NbtElement> entry : entries.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NbtCompound that)) return false;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return toSnbt();
    }
}
