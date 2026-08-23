package org.maboroshi.vessel.storage.nbt;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Serializes NBT elements into valid Minecraft Stringified NBT (SNBT).
 */
public final class SnbtSerializer {
    private static final Pattern SIMPLE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9._+-]+$");

    private SnbtSerializer() {}

    public static String serialize(NbtElement element) {
        StringBuilder sb = new StringBuilder();
        serialize(element, sb);
        return sb.toString();
    }

    private static void serialize(NbtElement element, StringBuilder sb) {
        if (element == null) return;

        switch (element.getType()) {
            case BYTE -> {
                NbtPrimitive.NbtByte b = (NbtPrimitive.NbtByte) element;
                sb.append(b.getValue()).append('b');
            }
            case SHORT -> {
                NbtPrimitive.NbtShort s = (NbtPrimitive.NbtShort) element;
                sb.append(s.getValue()).append('s');
            }
            case INT -> {
                NbtPrimitive.NbtInt i = (NbtPrimitive.NbtInt) element;
                sb.append(i.getValue());
            }
            case LONG -> {
                NbtPrimitive.NbtLong l = (NbtPrimitive.NbtLong) element;
                sb.append(l.getValue()).append('L');
            }
            case FLOAT -> {
                NbtPrimitive.NbtFloat f = (NbtPrimitive.NbtFloat) element;
                sb.append(f.getValue()).append('f');
            }
            case DOUBLE -> {
                NbtPrimitive.NbtDouble d = (NbtPrimitive.NbtDouble) element;
                sb.append(d.getValue()).append('d');
            }
            case STRING -> {
                NbtPrimitive.NbtString str = (NbtPrimitive.NbtString) element;
                escapeString(str.getValue(), sb);
            }
            case BYTE_ARRAY -> {
                NbtPrimitive.NbtByteArray arr = (NbtPrimitive.NbtByteArray) element;
                sb.append("[B;");
                byte[] bytes = arr.getValue();
                for (int i = 0; i < bytes.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(bytes[i]).append('b');
                }
                sb.append(']');
            }
            case INT_ARRAY -> {
                NbtPrimitive.NbtIntArray arr = (NbtPrimitive.NbtIntArray) element;
                sb.append("[I;");
                int[] ints = arr.getValue();
                for (int i = 0; i < ints.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(ints[i]);
                }
                sb.append(']');
            }
            case LONG_ARRAY -> {
                NbtPrimitive.NbtLongArray arr = (NbtPrimitive.NbtLongArray) element;
                sb.append("[L;");
                long[] longs = arr.getValue();
                for (int i = 0; i < longs.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(longs[i]).append('L');
                }
                sb.append(']');
            }
            case LIST -> {
                NbtList list = (NbtList) element;
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    serialize(list.get(i), sb);
                }
                sb.append(']');
            }
            case COMPOUND -> {
                NbtCompound compound = (NbtCompound) element;
                sb.append('{');
                boolean first = true;
                for (Map.Entry<String, NbtElement> entry : compound.getEntries().entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    escapeKey(entry.getKey(), sb);
                    sb.append(':');
                    serialize(entry.getValue(), sb);
                }
                sb.append('}');
            }
            case END -> {}
        }
    }

    public static void escapeKey(String key, StringBuilder sb) {
        if (SIMPLE_KEY_PATTERN.matcher(key).matches()) {
            sb.append(key);
        } else {
            escapeString(key, sb);
        }
    }

    public static void escapeString(String str, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }
}
