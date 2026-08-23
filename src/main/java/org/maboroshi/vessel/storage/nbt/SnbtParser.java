package org.maboroshi.vessel.storage.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.maboroshi.vessel.storage.VesselDataException;
import org.maboroshi.vessel.storage.VesselDataException.Reason;

/**
 * A fast, recursive descent parser for Minecraft Stringified NBT (SNBT).
 */
public final class SnbtParser {
    private static final Pattern INT_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("^-?\\d*\\.\\d+$");

    private final String input;
    private final int length;
    private int cursor;

    private SnbtParser(String input) {
        this.input = input;
        this.length = input != null ? input.length() : 0;
        this.cursor = 0;
    }

    public static NbtCompound parseCompound(String snbt) throws VesselDataException {
        if (snbt == null || snbt.isBlank()) {
            throw new VesselDataException(Reason.MALFORMED, "Empty or null SNBT string");
        }
        SnbtParser parser = new SnbtParser(snbt);
        parser.skipWhitespace();
        NbtElement element = parser.parseValue();
        parser.skipWhitespace();
        if (parser.hasRemaining()) {
            throw new VesselDataException(
                    Reason.MALFORMED, "Trailing characters after SNBT compound at position " + parser.cursor);
        }
        if (!(element instanceof NbtCompound compound)) {
            throw new VesselDataException(Reason.MALFORMED, "Root SNBT element is not a compound");
        }
        return compound;
    }

    public static NbtElement parse(String snbt) throws VesselDataException {
        if (snbt == null || snbt.isBlank()) {
            throw new VesselDataException(Reason.MALFORMED, "Empty or null SNBT string");
        }
        SnbtParser parser = new SnbtParser(snbt);
        parser.skipWhitespace();
        NbtElement element = parser.parseValue();
        parser.skipWhitespace();
        if (parser.hasRemaining()) {
            throw new VesselDataException(
                    Reason.MALFORMED, "Trailing characters after SNBT element at position " + parser.cursor);
        }
        return element;
    }

    private NbtElement parseValue() throws VesselDataException {
        skipWhitespace();
        if (!hasRemaining()) {
            throw new VesselDataException(Reason.MALFORMED, "Unexpected end of SNBT at position " + cursor);
        }

        char c = peek();
        if (c == '{') {
            return parseCompoundInternal();
        } else if (c == '[') {
            return parseListOrArray();
        } else if (c == '"' || c == '\'') {
            return new NbtPrimitive.NbtString(parseQuotedString());
        } else {
            return parseUnquoted();
        }
    }

    private NbtCompound parseCompoundInternal() throws VesselDataException {
        expect('{');
        skipWhitespace();

        NbtCompound compound = new NbtCompound();
        if (hasRemaining() && peek() == '}') {
            read(); // consume '}'
            return compound;
        }

        while (hasRemaining()) {
            skipWhitespace();
            if (peek() == '}') {
                read();
                return compound;
            }

            String key = parseKey();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            NbtElement value = parseValue();
            compound.put(key, value);

            skipWhitespace();
            if (hasRemaining() && peek() == ',') {
                read(); // consume ','
                skipWhitespace();
            } else if (hasRemaining() && peek() == '}') {
                read(); // consume '}'
                return compound;
            } else {
                throw new VesselDataException(
                        Reason.MALFORMED, "Expected ',' or '}' in compound at position " + cursor);
            }
        }

        throw new VesselDataException(Reason.MALFORMED, "Unclosed compound tag at position " + cursor);
    }

    private NbtElement parseListOrArray() throws VesselDataException {
        expect('[');
        skipWhitespace();

        if (hasRemaining() && peek() == ']') {
            read(); // empty list
            return new NbtList();
        }

        // Check for typed array prefix: [B;, [I;, [L;
        if (cursor + 1 < length && input.charAt(cursor + 1) == ';') {
            char arrayType = input.charAt(cursor);
            if (arrayType == 'B' || arrayType == 'I' || arrayType == 'L') {
                read(); // consume type char
                read(); // consume ';'
                skipWhitespace();
                return parseTypedArray(arrayType);
            }
        }

        // Standard NBT list
        NbtList list = new NbtList();
        while (hasRemaining()) {
            skipWhitespace();
            if (peek() == ']') {
                read();
                return list;
            }

            NbtElement element = parseValue();
            list.add(element);

            skipWhitespace();
            if (hasRemaining() && peek() == ',') {
                read();
                skipWhitespace();
            } else if (hasRemaining() && peek() == ']') {
                read();
                return list;
            } else {
                throw new VesselDataException(Reason.MALFORMED, "Expected ',' or ']' in list at position " + cursor);
            }
        }

        throw new VesselDataException(Reason.MALFORMED, "Unclosed list tag at position " + cursor);
    }

    private NbtElement parseTypedArray(char type) throws VesselDataException {
        if (peek() == ']') {
            read();
            return switch (type) {
                case 'B' -> new NbtPrimitive.NbtByteArray(new byte[0]);
                case 'I' -> new NbtPrimitive.NbtIntArray(new int[0]);
                case 'L' -> new NbtPrimitive.NbtLongArray(new long[0]);
                default -> throw new VesselDataException(Reason.MALFORMED, "Unknown array type: " + type);
            };
        }

        List<NbtElement> elements = new ArrayList<>();
        while (hasRemaining()) {
            skipWhitespace();
            if (peek() == ']') {
                read();
                break;
            }

            NbtElement value = parseValue();
            elements.add(value);

            skipWhitespace();
            if (hasRemaining() && peek() == ',') {
                read();
                skipWhitespace();
            } else if (hasRemaining() && peek() == ']') {
                read();
                break;
            } else {
                throw new VesselDataException(Reason.MALFORMED, "Expected ',' or ']' in array at position " + cursor);
            }
        }

        return switch (type) {
            case 'B' -> {
                byte[] arr = new byte[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    NbtElement el = elements.get(i);
                    if (el instanceof NbtPrimitive.NbtByte b) {
                        arr[i] = b.getValue();
                    } else if (el instanceof NbtPrimitive.NbtInt intEl) {
                        arr[i] = (byte) intEl.getValue();
                    } else {
                        throw new VesselDataException(Reason.MALFORMED, "Invalid byte array element: " + el);
                    }
                }
                yield new NbtPrimitive.NbtByteArray(arr);
            }
            case 'I' -> {
                int[] arr = new int[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    NbtElement el = elements.get(i);
                    if (el instanceof NbtPrimitive.NbtInt intEl) {
                        arr[i] = intEl.getValue();
                    } else if (el instanceof NbtPrimitive.NbtByte b) {
                        arr[i] = b.getValue();
                    } else if (el instanceof NbtPrimitive.NbtShort s) {
                        arr[i] = s.getValue();
                    } else {
                        throw new VesselDataException(Reason.MALFORMED, "Invalid int array element: " + el);
                    }
                }
                yield new NbtPrimitive.NbtIntArray(arr);
            }
            case 'L' -> {
                long[] arr = new long[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    NbtElement el = elements.get(i);
                    if (el instanceof NbtPrimitive.NbtLong l) {
                        arr[i] = l.getValue();
                    } else if (el instanceof NbtPrimitive.NbtInt intEl) {
                        arr[i] = intEl.getValue();
                    } else {
                        throw new VesselDataException(Reason.MALFORMED, "Invalid long array element: " + el);
                    }
                }
                yield new NbtPrimitive.NbtLongArray(arr);
            }
            default -> throw new VesselDataException(Reason.MALFORMED, "Unknown array type: " + type);
        };
    }

    private String parseKey() throws VesselDataException {
        skipWhitespace();
        if (!hasRemaining()) {
            throw new VesselDataException(Reason.MALFORMED, "Expected key at position " + cursor);
        }
        char c = peek();
        if (c == '"' || c == '\'') {
            return parseQuotedString();
        }
        return parseUnquotedKey();
    }

    private String parseUnquotedKey() {
        int start = cursor;
        while (hasRemaining()) {
            char c = peek();
            if (isAllowedInKey(c)) {
                read();
            } else {
                break;
            }
        }
        return input.substring(start, cursor);
    }

    private static boolean isAllowedInKey(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_'
                || c == '-'
                || c == '.'
                || c == '+';
    }

    private String parseQuotedString() throws VesselDataException {
        char quote = read();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        while (hasRemaining()) {
            char c = read();
            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else sb.append(c); // includes quotes, backslashes, etc.
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == quote) {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }

        throw new VesselDataException(Reason.MALFORMED, "Unclosed quoted string starting with " + quote);
    }

    private NbtElement parseUnquoted() {
        String token = parseUnquotedToken();

        if (token.equalsIgnoreCase("true")) {
            return new NbtPrimitive.NbtByte((byte) 1);
        }
        if (token.equalsIgnoreCase("false")) {
            return new NbtPrimitive.NbtByte((byte) 0);
        }

        int len = token.length();
        if (len > 1) {
            char last = token.charAt(len - 1);
            String numberPart = token.substring(0, len - 1);

            if (last == 'b' || last == 'B') {
                try {
                    return new NbtPrimitive.NbtByte(Byte.parseByte(numberPart));
                } catch (NumberFormatException ignored) {
                }
            } else if (last == 's' || last == 'S') {
                try {
                    return new NbtPrimitive.NbtShort(Short.parseShort(numberPart));
                } catch (NumberFormatException ignored) {
                }
            } else if (last == 'l' || last == 'L') {
                try {
                    return new NbtPrimitive.NbtLong(Long.parseLong(numberPart));
                } catch (NumberFormatException ignored) {
                }
            } else if (last == 'f' || last == 'F') {
                try {
                    return new NbtPrimitive.NbtFloat(Float.parseFloat(numberPart));
                } catch (NumberFormatException ignored) {
                }
            } else if (last == 'd' || last == 'D') {
                try {
                    return new NbtPrimitive.NbtDouble(Double.parseDouble(numberPart));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Check for float/double special constants
        if (token.equalsIgnoreCase("infinityf") || token.equalsIgnoreCase("+infinityf")) {
            return new NbtPrimitive.NbtFloat(Float.POSITIVE_INFINITY);
        }
        if (token.equalsIgnoreCase("-infinityf")) {
            return new NbtPrimitive.NbtFloat(Float.NEGATIVE_INFINITY);
        }
        if (token.equalsIgnoreCase("nanf")) {
            return new NbtPrimitive.NbtFloat(Float.NaN);
        }
        if (token.equalsIgnoreCase("infinityd")
                || token.equalsIgnoreCase("+infinityd")
                || token.equalsIgnoreCase("infinity")) {
            return new NbtPrimitive.NbtDouble(Double.POSITIVE_INFINITY);
        }
        if (token.equalsIgnoreCase("-infinityd") || token.equalsIgnoreCase("-infinity")) {
            return new NbtPrimitive.NbtDouble(Double.NEGATIVE_INFINITY);
        }
        if (token.equalsIgnoreCase("nand") || token.equalsIgnoreCase("nan")) {
            return new NbtPrimitive.NbtDouble(Double.NaN);
        }

        // Check for standard integers
        if (INT_PATTERN.matcher(token).matches()) {
            try {
                return new NbtPrimitive.NbtInt(Integer.parseInt(token));
            } catch (NumberFormatException ex1) {
                try {
                    return new NbtPrimitive.NbtLong(Long.parseLong(token));
                } catch (NumberFormatException ex2) {
                }
            }
        }

        // Check for standard doubles without suffix (e.g. 10.5, -0.25)
        if (DOUBLE_PATTERN.matcher(token).matches()) {
            try {
                return new NbtPrimitive.NbtDouble(Double.parseDouble(token));
            } catch (NumberFormatException ignored) {
            }
        }

        // Fallback to unquoted string (e.g. minecraft:cow)
        return new NbtPrimitive.NbtString(token);
    }

    private String parseUnquotedToken() {
        int start = cursor;
        while (hasRemaining()) {
            char c = peek();
            if (isAllowedInUnquoted(c)) {
                read();
            } else {
                break;
            }
        }
        return input.substring(start, cursor);
    }

    private static boolean isAllowedInUnquoted(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_'
                || c == '-'
                || c == '.'
                || c == '+'
                || c == ':';
    }

    private void skipWhitespace() {
        while (hasRemaining() && Character.isWhitespace(peek())) {
            read();
        }
    }

    private boolean hasRemaining() {
        return cursor < length;
    }

    private char peek() {
        return input.charAt(cursor);
    }

    private char read() {
        return input.charAt(cursor++);
    }

    private void expect(char expected) throws VesselDataException {
        if (!hasRemaining()) {
            throw new VesselDataException(
                    Reason.MALFORMED, "Expected '" + expected + "' but reached end of input at position " + cursor);
        }
        char actual = read();
        if (actual != expected) {
            throw new VesselDataException(
                    Reason.MALFORMED,
                    "Expected '" + expected + "' but found '" + actual + "' at position " + (cursor - 1));
        }
    }
}
