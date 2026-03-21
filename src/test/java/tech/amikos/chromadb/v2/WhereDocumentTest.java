package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WhereDocumentTest {

    // --- fromMap tests (kept unchanged) ---

    @Test
    public void testFromMapFactoryCopiesInput() {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("$contains", "hello");

        WhereDocument fromMap = WhereDocument.fromMap(source);
        source.put("$contains", "changed");

        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$contains", "hello");
        assertEquals(expected, fromMap.toMap());
    }

    @Test
    public void testFromMapFactoryRejectsNull() {
        try {
            WhereDocument.fromMap(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFromMapFactoryRejectsNonStringKeys() {
        Map raw = new LinkedHashMap();
        raw.put(Integer.valueOf(1), "bad");
        try {
            WhereDocument.fromMap((Map<String, Object>) raw);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFromMapFactoryRejectsNestedNonStringKeys() {
        Map nestedRaw = new LinkedHashMap();
        nestedRaw.put(Integer.valueOf(1), "bad");

        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("$and", Collections.<Object>singletonList(nestedRaw));
        try {
            WhereDocument.fromMap(source);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // --- Serialization tests ---

    @Test
    public void testContainsSerializesToMap() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$contains", "hello");
        assertEquals(expected, WhereDocument.contains("hello").toMap());
    }

    @Test
    public void testNotContainsSerializesToMap() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$not_contains", "hello");
        assertEquals(expected, WhereDocument.notContains("hello").toMap());
    }

    @Test
    public void testRegexSerializesToMap() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$regex", "\\bAI\\b");
        assertEquals(expected, WhereDocument.regex("\\bAI\\b").toMap());
    }

    @Test
    public void testNotRegexSerializesToMap() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$not_regex", "\\bAI\\b");
        assertEquals(expected, WhereDocument.notRegex("\\bAI\\b").toMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAndSerializesNestedConditions() {
        WhereDocument result = WhereDocument.and(
            WhereDocument.contains("hello"),
            WhereDocument.notContains("bye")
        );
        Map<String, Object> map = result.toMap();
        List<Map<String, Object>> clauses = (List<Map<String, Object>>) map.get("$and");
        assertEquals(2, clauses.size());
        assertEquals(Collections.singletonMap("$contains", "hello"), clauses.get(0));
        assertEquals(Collections.singletonMap("$not_contains", "bye"), clauses.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrSerializesNestedConditions() {
        WhereDocument result = WhereDocument.or(
            WhereDocument.regex("x"),
            WhereDocument.regex("y")
        );
        Map<String, Object> map = result.toMap();
        List<Map<String, Object>> clauses = (List<Map<String, Object>>) map.get("$or");
        assertEquals(2, clauses.size());
        assertEquals(Collections.singletonMap("$regex", "x"), clauses.get(0));
        assertEquals(Collections.singletonMap("$regex", "y"), clauses.get(1));
    }

    @Test
    public void testInstanceAndDelegatesToStaticAnd() {
        WhereDocument a = WhereDocument.contains("hello");
        WhereDocument b = WhereDocument.notContains("bye");
        assertEquals(WhereDocument.and(a, b).toMap(), a.and(b).toMap());
    }

    @Test
    public void testInstanceOrDelegatesToStaticOr() {
        WhereDocument a = WhereDocument.contains("hello");
        WhereDocument b = WhereDocument.notContains("bye");
        assertEquals(WhereDocument.or(a, b).toMap(), a.or(b).toMap());
    }

    // --- Validation tests ---

    @Test
    public void testContainsRejectsNull() {
        try { WhereDocument.contains(null); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testContainsRejectsBlank() {
        try { WhereDocument.contains(""); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
        try { WhereDocument.contains("   "); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testContainsPreservesWhitespace() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$contains", "  hello  ");
        assertEquals(expected, WhereDocument.contains("  hello  ").toMap());
    }

    @Test
    public void testNotContainsPreservesWhitespace() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$not_contains", "  bye  ");
        assertEquals(expected, WhereDocument.notContains("  bye  ").toMap());
    }

    @Test
    public void testNotContainsRejectsNull() {
        try { WhereDocument.notContains(null); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testRegexRejectsNull() {
        try { WhereDocument.regex(null); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testRegexAllowsEmptyString() {
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$regex", "");
        assertEquals(expected, WhereDocument.regex("").toMap());
    }

    @Test
    public void testNotRegexRejectsNull() {
        try { WhereDocument.notRegex(null); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testAndRejectsEmptyVarargs() {
        try { WhereDocument.and(); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testAndRejectsSingleClause() {
        try { WhereDocument.and(new WhereDocument[]{WhereDocument.contains("a")}); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testOrRejectsSingleClause() {
        try { WhereDocument.or(new WhereDocument[]{WhereDocument.contains("a")}); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testAndRejectsNullElement() {
        try { WhereDocument.and(WhereDocument.contains("a"), null); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    public void testOrRejectsEmptyVarargs() {
        try { WhereDocument.or(); fail("Expected IAE"); }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThreeWayAndSerializesCorrectly() {
        WhereDocument result = WhereDocument.and(
            WhereDocument.contains("a"),
            WhereDocument.notContains("b"),
            WhereDocument.regex("c")
        );
        List<Map<String, Object>> clauses = (List<Map<String, Object>>) result.toMap().get("$and");
        assertEquals(3, clauses.size());
    }
}
