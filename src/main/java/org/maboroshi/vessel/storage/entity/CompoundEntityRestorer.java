package org.maboroshi.vessel.storage.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.vessel.util.Keys;
import org.maboroshi.vessel.util.Log;

/**
 * Handles the transactional, Folia-safe restoration of compound entity trees.
 *
 * <p>Spawns each entity node individually at {@code loc} within the owning Region thread
 * (ensuring no entity is ever spawned with snapshot coordinates in a foreign region),
 * and only establishes {@code addPassenger} relationships after all nodes have successfully spawned.
 * If any node fails to spawn, all previously spawned entities in this operation are rolled back.
 */
public final class CompoundEntityRestorer {
    private CompoundEntityRestorer() {}

    public record RestoreResult(
            Entity rootEntity, List<Entity> allSpawnedEntities, boolean success, String errorMessage) {}

    /**
     * Atomically restores a compound entity tree at {@code loc}.
     *
     * @param tree the decomposed entity tree
     * @param loc the target release location (must be owned by the current region)
     * @param spawnReason creature spawn reason
     * @param savedReason original spawn reason to restore onto PDC, or null
     * @return the result of the restoration operation
     */
    public static RestoreResult restore(
            CompoundEntityTree tree, Location loc, CreatureSpawnEvent.SpawnReason spawnReason, String savedReason) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(loc, "loc");

        List<Entity> spawned = new ArrayList<>();
        try {
            Entity root = spawnNode(tree, loc, spawnReason, savedReason, spawned);
            if (root == null) {
                rollback(spawned);
                return new RestoreResult(null, Collections.emptyList(), false, "One or more entities failed to spawn");
            }

            // All entities successfully spawned at loc in the current region thread.
            // Now establish passenger relationships in topological order.
            attachPassengers(tree);

            return new RestoreResult(root, Collections.unmodifiableList(spawned), true, null);
        } catch (Exception e) {
            Log.error("Exception during compound entity restore, rolling back: " + e.getMessage());
            rollback(spawned);
            return new RestoreResult(null, Collections.emptyList(), false, e.getMessage());
        }
    }

    private static Entity spawnNode(
            CompoundEntityTree node,
            Location loc,
            CreatureSpawnEvent.SpawnReason spawnReason,
            String savedReason,
            List<Entity> spawned) {
        EntitySnapshot snapshot = node.getSnapshot();
        if (snapshot == null) {
            snapshot = Bukkit.getEntityFactory().createEntitySnapshot(node.getSnbtPayload());
            node.setSnapshot(snapshot);
        }

        Entity entity = snapshot.createEntity(loc.getWorld());
        if (entity == null) {
            Log.debug("Failed to instantiate entity from snapshot: " + node.getEntityType());
            return null;
        }

        if (!entity.spawnAt(loc, spawnReason)) {
            Log.debug("spawnAt returned false for entity: " + node.getEntityType());
            return null;
        }

        spawned.add(entity);
        node.setSpawnedEntity(entity);

        entity.getPersistentDataContainer().set(Keys.FROM_VESSEL, PersistentDataType.BOOLEAN, true);
        if (savedReason != null && !savedReason.isBlank()) {
            entity.getPersistentDataContainer()
                    .set(Keys.SPAWN_REASON, PersistentDataType.STRING, savedReason.toUpperCase(Locale.ROOT));
        }

        // Recursively spawn all passenger children at the same location in this region thread
        for (CompoundEntityTree child : node.getChildren()) {
            Entity childEntity = spawnNode(child, loc, spawnReason, savedReason, spawned);
            if (childEntity == null) {
                return null;
            }
        }

        return entity;
    }

    private static void attachPassengers(CompoundEntityTree node) {
        Entity parent = node.getSpawnedEntity();
        if (parent == null || !parent.isValid()) return;

        for (CompoundEntityTree child : node.getChildren()) {
            Entity childEntity = child.getSpawnedEntity();
            if (childEntity != null && childEntity.isValid()) {
                parent.addPassenger(childEntity);
                attachPassengers(child);
            }
        }
    }

    private static void rollback(List<Entity> spawned) {
        for (Entity entity : spawned) {
            try {
                if (entity != null && entity.isValid()) {
                    if (entity.isInsideVehicle()) {
                        entity.leaveVehicle();
                    }
                    for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
                        entity.removePassenger(passenger);
                    }
                    entity.remove();
                }
            } catch (Exception e) {
                Log.error("Error rolling back entity during failed restore: " + e.getMessage());
            }
        }
    }
}
