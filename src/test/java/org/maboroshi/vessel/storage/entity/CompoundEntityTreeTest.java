package org.maboroshi.vessel.storage.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.maboroshi.vessel.storage.VesselDataException;

class CompoundEntityTreeTest {

    @Test
    void parseSingleEntity() throws VesselDataException {
        String snbt = "{id:\"minecraft:cow\",Health:10.0f,Pos:[100.0d,64.0d,100.0d],Motion:[0.0d,0.0d,0.0d]}";
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(snbt);

        assertEquals("COW", tree.getEntityType());
        assertFalse(tree.hasPassengers());
        assertEquals(1, tree.totalEntityCount());
        assertEquals(1, tree.flatten().size());
        assertEquals(List.of("COW"), tree.allEntityTypes());

        // Position and motion tags must be stripped
        assertNull(tree.getEntityNbt().get("Pos"));
        assertNull(tree.getEntityNbt().get("Motion"));
        assertNull(tree.getEntityNbt().get("Passengers"));
        assertFalse(tree.getSnbtPayload().contains("Pos"));
    }

    @Test
    void parseOneLevelPassenger() throws VesselDataException {
        String snbt = "{"
                + "id:\"minecraft:camel\",Health:32.0f,Pos:[100.0d,64.0d,100.0d],"
                + "Passengers:[{id:\"minecraft:husk\",Health:20.0f,Pos:[100.0d,64.0d,100.0d]}]"
                + "}";
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(snbt);

        assertEquals("CAMEL", tree.getEntityType());
        assertTrue(tree.hasPassengers());
        assertEquals(2, tree.totalEntityCount());
        assertEquals(1, tree.getChildren().size());

        CompoundEntityTree huskNode = tree.getChildren().get(0);
        assertEquals("HUSK", huskNode.getEntityType());
        assertFalse(huskNode.hasPassengers());

        assertEquals(List.of("CAMEL", "HUSK"), tree.allEntityTypes());

        // Ensure neither node's payload contains Passengers or Pos
        assertNull(tree.getEntityNbt().get("Passengers"));
        assertNull(tree.getEntityNbt().get("Pos"));
        assertNull(huskNode.getEntityNbt().get("Passengers"));
        assertNull(huskNode.getEntityNbt().get("Pos"));

        assertFalse(tree.getSnbtPayload().contains("Passengers"));
        assertFalse(huskNode.getSnbtPayload().contains("Passengers"));
    }

    @Test
    void parseTwoLevelPassengerStack() throws VesselDataException {
        String snbt = "{"
                + "id:\"minecraft:camel\",Health:32.0f,"
                + "Passengers:[{"
                + "  id:\"minecraft:husk\",Health:20.0f,"
                + "  Passengers:[{"
                + "    id:\"minecraft:bogged\",Health:16.0f"
                + "  }]"
                + "}]"
                + "}";
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(snbt);

        assertEquals("CAMEL", tree.getEntityType());
        assertEquals(3, tree.totalEntityCount());
        assertEquals(List.of("CAMEL", "HUSK", "BOGGED"), tree.allEntityTypes());

        CompoundEntityTree huskNode = tree.getChildren().get(0);
        assertEquals("HUSK", huskNode.getEntityType());
        assertEquals(1, huskNode.getChildren().size());

        CompoundEntityTree boggedNode = huskNode.getChildren().get(0);
        assertEquals("BOGGED", boggedNode.getEntityType());
        assertEquals(0, boggedNode.getChildren().size());

        // All nodes must be clean of Passengers tag in their single-entity payload
        assertFalse(tree.getSnbtPayload().contains("Passengers"));
        assertFalse(huskNode.getSnbtPayload().contains("Passengers"));
        assertFalse(boggedNode.getSnbtPayload().contains("Passengers"));
    }

    @Test
    void parseThreeLevelPassengerStack() throws VesselDataException {
        String snbt = "{"
                + "id:\"minecraft:camel\","
                + "Passengers:[{"
                + "  id:\"minecraft:husk\","
                + "  Passengers:[{"
                + "    id:\"minecraft:skeleton\","
                + "    Passengers:[{"
                + "      id:\"minecraft:bogged\""
                + "    }]"
                + "  }]"
                + "}]"
                + "}";
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(snbt);

        assertEquals(4, tree.totalEntityCount());
        assertEquals(List.of("CAMEL", "HUSK", "SKELETON", "BOGGED"), tree.allEntityTypes());
    }

    @Test
    void parseBranchingPassengers() throws VesselDataException {
        // Camel or Boat with two passengers on the root vehicle
        String snbt = "{"
                + "id:\"minecraft:boat\","
                + "Passengers:[{"
                + "  id:\"minecraft:zombie\",Health:20.0f"
                + "},{"
                + "  id:\"minecraft:skeleton\",Health:20.0f"
                + "}]"
                + "}";
        CompoundEntityTree tree = CompoundEntityTree.fromSnbt(snbt);

        assertEquals("BOAT", tree.getEntityType());
        assertEquals(3, tree.totalEntityCount());
        assertEquals(2, tree.getChildren().size());
        assertEquals("ZOMBIE", tree.getChildren().get(0).getEntityType());
        assertEquals("SKELETON", tree.getChildren().get(1).getEntityType());
        assertEquals(List.of("BOAT", "ZOMBIE", "SKELETON"), tree.allEntityTypes());
    }
}
