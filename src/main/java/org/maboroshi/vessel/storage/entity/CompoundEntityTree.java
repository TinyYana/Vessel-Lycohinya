package org.maboroshi.vessel.storage.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityFactory;
import org.bukkit.entity.EntitySnapshot;
import org.maboroshi.vessel.storage.VesselDataException;
import org.maboroshi.vessel.storage.nbt.NbtCompound;
import org.maboroshi.vessel.storage.nbt.NbtElement;
import org.maboroshi.vessel.storage.nbt.NbtList;
import org.maboroshi.vessel.storage.nbt.SnbtParser;
import org.maboroshi.vessel.storage.nbt.SnbtSerializer;

/**
 * Represents an entity node in a compound riding hierarchy.
 *
 * <p>When an entity has passengers, Paper/Minecraft stores them in the {@code Passengers} NBT list.
 * This class parses that hierarchy into a tree of decoupled entity nodes, stripping position and
 * passenger tags from each node so they can each be safely spawned independently at the target
 * location on Folia before their passenger relationships are re-established.
 */
public final class CompoundEntityTree {
    private final NbtCompound entityNbt;
    private final String entityType;
    private final String snbtPayload;
    private final List<CompoundEntityTree> children;

    private EntitySnapshot snapshot;
    private Entity spawnedEntity;

    public CompoundEntityTree(
            NbtCompound entityNbt, String entityType, String snbtPayload, List<CompoundEntityTree> children) {
        this.entityNbt = Objects.requireNonNull(entityNbt, "entityNbt");
        this.entityType = entityType != null ? entityType : "UNKNOWN";
        this.snbtPayload = Objects.requireNonNull(snbtPayload, "snbtPayload");
        this.children = new ArrayList<>(Objects.requireNonNull(children, "children"));
    }

    /**
     * Parses a full SNBT string (which may contain nested {@code Passengers}) into a {@link CompoundEntityTree}.
     *
     * @param fullSnbt the SNBT string
     * @return the parsed tree
     * @throws VesselDataException if the SNBT is malformed
     */
    public static CompoundEntityTree fromSnbt(String fullSnbt) throws VesselDataException {
        NbtCompound root = SnbtParser.parseCompound(fullSnbt);
        return fromNbt(root);
    }

    /**
     * Deconstructs an NBT compound into a {@link CompoundEntityTree} node, recursively deconstructing
     * any {@code Passengers} and stripping passenger/position tags from this node.
     *
     * @param nbt the raw entity compound
     * @return the tree node
     */
    public static CompoundEntityTree fromNbt(NbtCompound nbt) {
        NbtCompound nodeNbt = nbt.copy();

        // 1. Extract child passengers
        List<CompoundEntityTree> children = new ArrayList<>();
        NbtList passengers = nodeNbt.getList("Passengers");
        if (passengers != null) {
            for (NbtElement passengerEl : passengers) {
                if (passengerEl instanceof NbtCompound passengerCompound) {
                    children.add(fromNbt(passengerCompound));
                }
            }
        }

        // 2. Strip Passengers and position-dependent tags from this node
        nodeNbt.remove("Passengers");
        nodeNbt.remove("Pos");
        nodeNbt.remove("Motion");
        nodeNbt.remove("FallDistance");

        // 3. Extract entity type identifier
        String rawId = nodeNbt.getString("id");
        String resolvedType;
        if (rawId != null && !rawId.isBlank()) {
            String stripped = rawId.startsWith("minecraft:") ? rawId.substring("minecraft:".length()) : rawId;
            resolvedType = stripped.toUpperCase(Locale.ROOT);
        } else {
            resolvedType = "UNKNOWN";
        }

        // 4. Serialize stripped NBT for single-entity snapshot creation
        String strippedSnbt = SnbtSerializer.serialize(nodeNbt);

        return new CompoundEntityTree(nodeNbt, resolvedType, strippedSnbt, children);
    }

    public NbtCompound getEntityNbt() {
        return entityNbt;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getSnbtPayload() {
        return snbtPayload;
    }

    public List<CompoundEntityTree> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean hasPassengers() {
        return !children.isEmpty();
    }

    public int totalEntityCount() {
        int count = 1;
        for (CompoundEntityTree child : children) {
            count += child.totalEntityCount();
        }
        return count;
    }

    /**
     * Flattens the tree in pre-order (root first, followed by descendants).
     *
     * @return flat list of all nodes
     */
    public List<CompoundEntityTree> flatten() {
        List<CompoundEntityTree> list = new ArrayList<>();
        flattenInternal(list);
        return list;
    }

    private void flattenInternal(List<CompoundEntityTree> list) {
        list.add(this);
        for (CompoundEntityTree child : children) {
            child.flattenInternal(list);
        }
    }

    /**
     * Returns a list of all entity type names present in this riding tree.
     *
     * @return list of uppercase entity type names
     */
    public List<String> allEntityTypes() {
        List<String> types = new ArrayList<>();
        for (CompoundEntityTree node : flatten()) {
            types.add(node.getEntityType());
        }
        return types;
    }

    public EntitySnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(EntitySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Entity getSpawnedEntity() {
        return spawnedEntity;
    }

    public void setSpawnedEntity(Entity spawnedEntity) {
        this.spawnedEntity = spawnedEntity;
    }

    /**
     * Recursively populates {@link EntitySnapshot}s across this node and all descendants using
     * the provided {@link EntityFactory}.
     *
     * @param entityFactory the server EntityFactory
     */
    public void populateSnapshots(EntityFactory entityFactory) {
        this.snapshot = entityFactory.createEntitySnapshot(this.snbtPayload);
        for (CompoundEntityTree child : children) {
            child.populateSnapshots(entityFactory);
        }
    }
}
