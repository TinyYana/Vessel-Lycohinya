package org.maboroshi.vessel.storage.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Represents an NBT List tag (ordered sequence of NbtElements).
 */
public final class NbtList implements NbtElement, Iterable<NbtElement> {
    private final List<NbtElement> elements;

    public NbtList() {
        this.elements = new ArrayList<>();
    }

    public NbtList(List<NbtElement> initialElements) {
        this.elements = new ArrayList<>(initialElements);
    }

    @Override
    public NbtType getType() {
        return NbtType.LIST;
    }

    public List<NbtElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void add(NbtElement element) {
        Objects.requireNonNull(element, "element");
        elements.add(element);
    }

    public NbtElement get(int index) {
        return elements.get(index);
    }

    public NbtElement remove(int index) {
        return elements.remove(index);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public void clear() {
        elements.clear();
    }

    @Override
    public Iterator<NbtElement> iterator() {
        return elements.iterator();
    }

    @Override
    public NbtList copy() {
        NbtList copy = new NbtList();
        for (NbtElement el : elements) {
            copy.add(el.copy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NbtList that)) return false;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return toSnbt();
    }
}
