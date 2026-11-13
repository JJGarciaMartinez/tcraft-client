package hopsync.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal strict JSON parser (RFC 8259) with no external dependencies.
 * <p>
 * Values map to Java types as follows: object → {@code Map<String, Object>} (insertion ordered),
 * array → {@code List<Object>}, string → {@code String}, integer → {@code Long},
 * other numbers → {@code Double}, true/false → {@code Boolean}, null → {@code null}.
 * Returned collections are unmodifiable.
 */
public final class Json {
    private static final int MAX_DEPTH = 256;

    private final String text;
    private int pos;
    private int depth;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Parses a complete JSON document.
     *
     * @throws JsonParseException if the text is not valid JSON
     */
    public static Object parse(String text) throws JsonParseException {
        if (text == null) {
            throw new JsonParseException("JSON nulo", 0);
        }
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < text.length()) {
            throw parser.error("contenido inesperado después del JSON");
        }
        return value;
    }

    /**
     * Serializes maps, lists, strings, numbers, booleans and null to compact JSON.
     *
     * @throws IllegalArgumentException for any other type or a non-finite number
     */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> writeString(sb, s);
            case Boolean b -> sb.append(b);
            case Double d when !Double.isFinite(d) -> throw new IllegalArgumentException("Non-finite number: " + d);
            case Float f when !Float.isFinite(f) -> throw new IllegalArgumentException("Non-finite number: " + f);
            case Number n -> sb.append(n);
            case Map<?, ?> map -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalArgumentException("JSON object keys must be strings: " + entry.getKey());
                    }
                    if (!first) sb.append(',');
                    first = false;
                    writeString(sb, key);
                    sb.append(':');
                    writeValue(sb, entry.getValue());
                }
                sb.append('}');
            }
            case List<?> list -> {
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    writeValue(sb, list.get(i));
                }
                sb.append(']');
            }
            default -> throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass().getName());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private Object readValue() throws JsonParseException {
        if (pos >= text.length()) {
            throw error("fin inesperado del JSON");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) yield readNumber();
                throw error("carácter inesperado '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() throws JsonParseException {
        enter();
        pos++; // '{'
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            depth--;
            return Collections.unmodifiableMap(map);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("se esperaba una clave entre comillas");
            }
            int keyPos = pos;
            String key = readString();
            if (map.containsKey(key)) {
                pos = keyPos;
                throw error("clave duplicada \"" + key + "\"");
            }
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') break;
            if (c != ',') {
                pos--;
                throw error("se esperaba ',' o '}'");
            }
        }
        depth--;
        return Collections.unmodifiableMap(map);
    }

    private List<Object> readArray() throws JsonParseException {
        enter();
        pos++; // '['
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            depth--;
            return Collections.unmodifiableList(list);
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') break;
            if (c != ',') {
                pos--;
                throw error("se esperaba ',' o ']'");
            }
        }
        depth--;
        return Collections.unmodifiableList(list);
    }

    private String readString() throws JsonParseException {
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("cadena sin cerrar");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                pos--;
                throw error("carácter de control sin escapar en cadena");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char esc = next();
            switch (esc) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> sb.append(readUnicodeEscape());
                default -> {
                    pos--;
                    throw error("secuencia de escape inválida '\\" + esc + "'");
                }
            }
        }
    }

    private char readUnicodeEscape() throws JsonParseException {
        if (pos + 4 > text.length()) {
            throw error("escape \\u incompleto");
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(text.charAt(pos + i), 16);
            if (digit < 0) {
                throw error("escape \\u con dígito hexadecimal inválido");
            }
            value = value * 16 + digit;
        }
        pos += 4;
        return (char) value;
    }

    private Object readNumber() throws JsonParseException {
        int start = pos;
        if (peek() == '-') pos++;
        if (peek() == '0') {
            pos++;
        } else if (isDigit(peek())) {
            while (isDigit(peek())) pos++;
        } else {
            throw error("número inválido");
        }
        boolean integral = true;
        if (peek() == '.') {
            integral = false;
            pos++;
            if (!isDigit(peek())) throw error("se esperaba un dígito después del punto decimal");
            while (isDigit(peek())) pos++;
        }
        if (peek() == 'e' || peek() == 'E') {
            integral = false;
            pos++;
            if (peek() == '+' || peek() == '-') pos++;
            if (!isDigit(peek())) throw error("exponente inválido");
            while (isDigit(peek())) pos++;
        }
        String number = text.substring(start, pos);
        if (integral) {
            try {
                return Long.parseLong(number);
            } catch (NumberFormatException e) {
                // Too large for a long: fall back to double like most JSON parsers
            }
        }
        return Double.parseDouble(number);
    }

    private Object readLiteral(String literal, Object value) throws JsonParseException {
        if (!text.startsWith(literal, pos)) {
            throw error("literal inválido");
        }
        pos += literal.length();
        return value;
    }

    private void enter() throws JsonParseException {
        if (++depth > MAX_DEPTH) {
            throw error("anidamiento demasiado profundo");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return;
            pos++;
        }
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private char next() throws JsonParseException {
        if (pos >= text.length()) {
            throw error("fin inesperado del JSON");
        }
        return text.charAt(pos++);
    }

    private void expect(char expected) throws JsonParseException {
        if (peek() != expected) {
            throw error("se esperaba '" + expected + "'");
        }
        pos++;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message, pos);
    }
}
