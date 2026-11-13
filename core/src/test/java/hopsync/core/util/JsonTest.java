package hopsync.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesNestedStructures() throws Exception {
        Object value = Json.parse("{\"a\": [1, 2.5, -3e2, true, false, null], \"b\": {\"c\": \"d\"}, \"e\": []}");

        Map<?, ?> root = (Map<?, ?>) value;
        assertEquals(Arrays.asList(1L, 2.5, -300.0, true, false, null), root.get("a"));
        assertEquals(Map.of("c", "d"), root.get("b"));
        assertEquals(List.of(), root.get("e"));
    }

    @Test
    void preservesKeyOrder() throws Exception {
        Map<?, ?> root = (Map<?, ?>) Json.parse("{\"z\": 1, \"a\": 2, \"m\": 3}");
        assertEquals(List.of("z", "a", "m"), List.copyOf(root.keySet()));
    }

    @Test
    void decodesEscapes() throws Exception {
        assertEquals("q\" b\\ s/ \b\f\n\r\t é 😀", Json.parse("\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u00e9 \\ud83d\\ude00\""));
    }

    @Test
    void keepsRawUnicode() throws Exception {
        assertEquals("¡Actualización! 日本", Json.parse("\"¡Actualización! 日本\""));
    }

    @Test
    void parsesNumbers() throws Exception {
        assertEquals(0L, Json.parse("0"));
        assertEquals(-12L, Json.parse("-12"));
        assertEquals(9223372036854775807L, Json.parse("9223372036854775807"));
        assertEquals(1e20, Json.parse("100000000000000000000"));
        assertEquals(0.5, Json.parse("0.5"));
        assertEquals(1.5e-3, Json.parse("1.5E-3"));
    }

    @Test
    void parsesTopLevelNullAndWhitespace() throws Exception {
        assertNull(Json.parse(" \n\t null \r\n"));
    }

    @Test
    void returnsUnmodifiableCollections() throws Exception {
        Map<?, ?> root = (Map<?, ?>) Json.parse("{\"a\": [1]}");
        assertThrows(UnsupportedOperationException.class, () -> root.clear());
        assertThrows(UnsupportedOperationException.class, () -> ((List<?>) root.get("a")).clear());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "   ", "{", "}", "[1,]", "[1 2]", "{\"a\" 1}", "{\"a\":1,}", "{a:1}", "{'a':1}",
        "\"unterminated", "\"bad \\x escape\"", "\"\\u12\"", "\"\\uZZZZ\"", "\"tab\there\"",
        "01", "-", "1.", ".5", "1e", "+1", "tru", "nul", "NaN", "Infinity",
        "{\"a\":1}x", "[1] [2]", "{\"a\":1,\"a\":2}"
    })
    void rejectsInvalidJson(String json) {
        assertThrows(JsonParseException.class, () -> Json.parse(json));
    }

    @Test
    void rejectsNull() {
        assertThrows(JsonParseException.class, () -> Json.parse(null));
    }

    @Test
    void rejectsExcessiveNesting() {
        String deep = "[".repeat(10_000) + "]".repeat(10_000);
        JsonParseException e = assertThrows(JsonParseException.class, () -> Json.parse(deep));
        assertTrue(e.getMessage().contains("anidamiento"));
    }

    @Test
    void reportsErrorPosition() {
        JsonParseException e = assertThrows(JsonParseException.class, () -> Json.parse("{\"a\": tru}"));
        assertEquals(6, e.getPosition());
    }

    @Test
    void writesAndReadsBack() throws Exception {
        Map<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("text", "q\" b\\ \n\t\u0001 é 😀");
        value.put("int", 42L);
        value.put("double", 1.5);
        value.put("bool", true);
        value.put("nothing", null);
        value.put("list", Arrays.asList(1L, "two", null));
        value.put("map", Map.of("k", "v"));

        String json = Json.write(value);

        assertEquals(value, Json.parse(json));
        assertTrue(json.contains("\\u0001"));
    }

    @Test
    void refusesUnsupportedValues() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.write(Map.of(1, "x")));
    }
}
