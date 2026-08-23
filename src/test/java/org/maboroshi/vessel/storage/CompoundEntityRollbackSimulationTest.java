package org.maboroshi.vessel.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.maboroshi.vessel.storage.entity.CompoundEntityTree;
import org.maboroshi.vessel.storage.nbt.NbtCompound;

class CompoundEntityRollbackSimulationTest {

    @Test
    void test30000BlocksDistancePositionStripping() throws VesselDataException {
        // Compound entity captured at (30000, 64, 30000)
        String originalCapturedSnbt = "{"
                + "id:\"minecraft:camel\",Health:32.0f,Pos:[30000.0d,64.0d,30000.0d],Motion:[0.0d,0.0d,0.0d],"
                + "Passengers:[{"
                + "  id:\"minecraft:husk\",Health:20.0f,Pos:[30000.0d,64.0d,30000.0d],"
                + "  Passengers:[{"
                + "    id:\"minecraft:bogged\",Health:16.0f,Pos:[30000.0d,64.0d,30000.0d]"
                + "  }]"
                + "}]"
                + "}";

        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(originalCapturedSnbt);

        assertEquals("CAMEL", tree.getEntityType());
        assertEquals(3, tree.totalEntityCount());

        // Verify that EVERY node has had Pos, Motion, FallDistance, and Passengers completely stripped
        for (CompoundEntityTree node : tree.flatten()) {
            NbtCompound nbt = node.getEntityNbt();
            assertNull(nbt.get("Pos"), "Node " + node.getEntityType() + " still had Pos tag");
            assertNull(nbt.get("Motion"), "Node " + node.getEntityType() + " still had Motion tag");
            assertNull(nbt.get("FallDistance"), "Node " + node.getEntityType() + " still had FallDistance tag");
            assertNull(nbt.get("Passengers"), "Node " + node.getEntityType() + " still had Passengers tag");

            String payload = node.getSnbtPayload();
            assertFalse(payload.contains("Pos"), "Node payload contained Pos");
            assertFalse(payload.contains("Passengers"), "Node payload contained Passengers");
        }
    }

    @Test
    void testPersistenceEnvelopeRoundTripForCompoundTree() throws VesselDataException {
        String compoundSnbt = "{"
                + "id:\"minecraft:camel\",Health:32.0f,"
                + "Passengers:[{"
                + "  id:\"minecraft:husk\",Health:20.0f,"
                + "  Passengers:[{"
                + "    id:\"minecraft:bogged\",Health:16.0f"
                + "  }]"
                + "}]"
                + "}";

        long checksum = ChecksumUtil.compute(compoundSnbt);
        VesselPayload payload = VesselPayloadStore.decodeEnvelope(
                EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION,
                4189,
                EntitySnapshotAdapter.CODEC_ID,
                "CAMEL",
                compoundSnbt,
                checksum,
                "test-vessel-uuid");

        assertEquals(EntitySnapshotAdapter.CURRENT_SCHEMA_VERSION, payload.schemaVersion());
        assertEquals("CAMEL", payload.entityType());
        assertEquals(compoundSnbt, payload.payload());
        assertEquals(checksum, payload.checksum());

        // Decode into compound tree
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(payload.payload());
        assertEquals(3, tree.totalEntityCount());
        assertEquals(List.of("CAMEL", "HUSK", "BOGGED"), tree.allEntityTypes());
    }

    @Test
    void testTreeStructureAndHierarchyMapping() throws VesselDataException {
        String treeSnbt = "{"
                + "id:\"minecraft:camel\","
                + "Passengers:[{"
                + "  id:\"minecraft:husk\","
                + "  Passengers:[{"
                + "    id:\"minecraft:bogged\""
                + "  }]"
                + "},{"
                + "  id:\"minecraft:zombie\""
                + "}]"
                + "}";

        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(treeSnbt);

        assertEquals("CAMEL", tree.getEntityType());
        assertEquals(2, tree.getChildren().size());

        CompoundEntityTree husk = tree.getChildren().get(0);
        assertEquals("HUSK", husk.getEntityType());
        assertEquals(1, husk.getChildren().size());

        CompoundEntityTree bogged = husk.getChildren().get(0);
        assertEquals("BOGGED", bogged.getEntityType());
        assertEquals(0, bogged.getChildren().size());

        CompoundEntityTree zombie = tree.getChildren().get(1);
        assertEquals("ZOMBIE", zombie.getEntityType());
        assertEquals(0, zombie.getChildren().size());

        assertEquals(4, tree.totalEntityCount());
    }

    @Test
    void testRollbackSimulationOnSpawnFailure() {
        // Simulate a mock spawned entity list during partial failure
        List<MockEntity> mockWorld = new ArrayList<>();
        List<MockEntity> spawned = new ArrayList<>();

        MockEntity camel = new MockEntity("CAMEL");
        mockWorld.add(camel);
        spawned.add(camel);

        MockEntity husk = new MockEntity("HUSK");
        mockWorld.add(husk);
        spawned.add(husk);

        // Third entity (Bogged) fails to spawn!
        // Rollback execution:
        for (MockEntity e : spawned) {
            e.leaveVehicle();
            e.remove();
            mockWorld.remove(e);
        }

        // Verify that world has 0 entities left and no partial graph remains
        assertTrue(mockWorld.isEmpty());
        assertFalse(camel.isValid());
        assertFalse(husk.isValid());
    }

    private static class MockEntity {
        private final String type;
        private boolean valid = true;
        private MockEntity vehicle = null;
        private final List<MockEntity> passengers = new ArrayList<>();

        MockEntity(String type) {
            this.type = type;
        }

        boolean isValid() {
            return valid;
        }

        void remove() {
            this.valid = false;
        }

        void leaveVehicle() {
            if (vehicle != null) {
                vehicle.passengers.remove(this);
                vehicle = null;
            }
        }
    }
}
