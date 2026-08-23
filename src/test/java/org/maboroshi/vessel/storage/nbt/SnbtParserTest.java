package org.maboroshi.vessel.storage.nbt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.maboroshi.vessel.storage.VesselDataException;

class SnbtParserTest {

    @Test
    void parseSimpleCompound() throws VesselDataException {
        String snbt = "{id:\"minecraft:cow\",Health:10.0f,Age:0,CustomName:'\"Bessie\"'}";
        NbtCompound compound = SnbtParser.parseCompound(snbt);

        assertEquals("minecraft:cow", compound.getString("id"));
        assertEquals(10.0f, compound.getFloat("Health"));
        assertEquals(0, compound.getInt("Age"));
        assertEquals("\"Bessie\"", compound.getString("CustomName"));
    }

    @Test
    void parseAllPrimitiveTypes() throws VesselDataException {
        String snbt = "{"
                + "b: 1b, "
                + "s: 100s, "
                + "i: 1000, "
                + "l: 100000L, "
                + "f: 3.14f, "
                + "d: 2.71828d, "
                + "str: \"hello world\", "
                + "bool_t: true, "
                + "bool_f: false"
                + "}";
        NbtCompound compound = SnbtParser.parseCompound(snbt);

        assertEquals((byte) 1, compound.getByte("b"));
        assertEquals((short) 100, compound.getShort("s"));
        assertEquals(1000, compound.getInt("i"));
        assertEquals(100000L, compound.getLong("l"));
        assertEquals(3.14f, compound.getFloat("f"));
        assertEquals(2.71828d, compound.getDouble("d"));
        assertEquals("hello world", compound.getString("str"));
        assertEquals((byte) 1, compound.getByte("bool_t"));
        assertEquals((byte) 0, compound.getByte("bool_f"));
    }

    @Test
    void parseArrays() throws VesselDataException {
        String snbt = "{bytes: [B; 1b, 2b, 3b], ints: [I; 10, 20, 30], longs: [L; 100L, 200L, 300L]}";
        NbtCompound compound = SnbtParser.parseCompound(snbt);

        NbtElement bytesEl = compound.get("bytes");
        assertInstanceOf(NbtPrimitive.NbtByteArray.class, bytesEl);
        byte[] bytes = ((NbtPrimitive.NbtByteArray) bytesEl).getValue();
        assertEquals(3, bytes.length);
        assertEquals(1, bytes[0]);
        assertEquals(2, bytes[1]);
        assertEquals(3, bytes[2]);

        NbtElement intsEl = compound.get("ints");
        assertInstanceOf(NbtPrimitive.NbtIntArray.class, intsEl);
        int[] ints = ((NbtPrimitive.NbtIntArray) intsEl).getValue();
        assertEquals(3, ints.length);
        assertEquals(10, ints[0]);
        assertEquals(20, ints[1]);
        assertEquals(30, ints[2]);

        NbtElement longsEl = compound.get("longs");
        assertInstanceOf(NbtPrimitive.NbtLongArray.class, longsEl);
        long[] longs = ((NbtPrimitive.NbtLongArray) longsEl).getValue();
        assertEquals(3, longs.length);
        assertEquals(100L, longs[0]);
        assertEquals(200L, longs[1]);
        assertEquals(300L, longs[2]);
    }

    @Test
    void parseNestedCompoundAndList() throws VesselDataException {
        String snbt =
                "{id:\"minecraft:camel\",Passengers:[{id:\"minecraft:husk\",Passengers:[{id:\"minecraft:bogged\"}]}]}";
        NbtCompound compound = SnbtParser.parseCompound(snbt);

        assertEquals("minecraft:camel", compound.getString("id"));
        NbtList passengers = compound.getList("Passengers");
        assertNotNull(passengers);
        assertEquals(1, passengers.size());

        NbtCompound husk = (NbtCompound) passengers.get(0);
        assertEquals("minecraft:husk", husk.getString("id"));
        NbtList huskPassengers = husk.getList("Passengers");
        assertNotNull(huskPassengers);
        assertEquals(1, huskPassengers.size());

        NbtCompound bogged = (NbtCompound) huskPassengers.get(0);
        assertEquals("minecraft:bogged", bogged.getString("id"));
    }

    @Test
    void serializeAndRoundTrip() throws VesselDataException {
        String snbt = "{id:\"minecraft:camel\",Health:32.0f,Passengers:[{id:\"minecraft:husk\",Health:20.0f}]}";
        NbtCompound original = SnbtParser.parseCompound(snbt);
        String serialized = SnbtSerializer.serialize(original);

        NbtCompound roundTripped = SnbtParser.parseCompound(serialized);
        assertEquals(original, roundTripped);
    }

    @Test
    void malformedSnbtThrowsException() {
        assertThrows(VesselDataException.class, () -> SnbtParser.parseCompound(""));
        assertThrows(VesselDataException.class, () -> SnbtParser.parseCompound("{unclosed"));
        assertThrows(VesselDataException.class, () -> SnbtParser.parseCompound("[1, 2"));
        assertThrows(VesselDataException.class, () -> SnbtParser.parseCompound("{id: \"cow\"} extra"));
    }
}
