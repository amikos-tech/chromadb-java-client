package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WhereTest {

    @Test
    public void testEqualityFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.eq("k", true); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.ne("k", false); } });
    }

    @Test
    public void testComparisonFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gt("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gt("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gte("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.gte("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lt("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lt("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lte("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.lte("k", 1.0f); } });
    }

    @Test
    public void testSetFactoriesThrowUnsupportedOperationException() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", "a", "b"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", 1, 2); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.in("k", 1.0f, 2.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", "a", "b"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", 1, 2); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.nin("k", 1.0f, 2.0f); } });
    }

    @Test
    public void testIdFactoriesSerializeToMap() {
        Map<String, Object> expectedIn = operatorCondition("#id", "$in", Arrays.asList("id1", "id2", "id3"));
        Map<String, Object> expectedNotIn = operatorCondition("#id", "$nin", Arrays.asList("id1", "id2"));

        assertEquals(expectedIn, Where.idIn("id1", "id2", "id3").toMap());
        assertEquals(expectedNotIn, Where.idNotIn("id1", "id2").toMap());
    }

    @Test
    public void testIdFactoriesSerializeSingleElement() {
        Map<String, Object> expected = operatorCondition("#id", "$in", Arrays.asList("id1"));
        assertEquals(expected, Where.idIn("id1").toMap());
    }

    @Test
    public void testIdFactoriesTrimValues() {
        Map<String, Object> expected = operatorCondition("#id", "$in", Arrays.asList("id1", "id2"));
        assertEquals(expected, Where.idIn("  id1 ", "\tid2\t").toMap());
    }

    @Test
    public void testInlineDocumentFactoriesSerializeToMap() {
        Map<String, Object> expectedContains = operatorCondition("#document", "$contains", "search text");
        Map<String, Object> expectedNotContains = operatorCondition("#document", "$not_contains", "excluded text");

        assertEquals(expectedContains, Where.documentContains("search text").toMap());
        assertEquals(expectedNotContains, Where.documentNotContains("excluded text").toMap());
    }

    @Test
    public void testInlineDocumentFactoriesTrimValues() {
        Map<String, Object> expectedContains = operatorCondition("#document", "$contains", "search text");
        Map<String, Object> expectedNotContains = operatorCondition("#document", "$not_contains", "excluded text");

        assertEquals(expectedContains, Where.documentContains("  search text ").toMap());
        assertEquals(expectedNotContains, Where.documentNotContains("\texcluded text\t").toMap());
    }

    @Test
    public void testArrayMetadataFactoriesRemainNotImplemented() {
        assertNotImplemented(new Runnable() { @Override public void run() { Where.contains("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.contains("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.contains("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.contains("k", true); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.notContains("k", "v"); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.notContains("k", 1); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.notContains("k", 1.0f); } });
        assertNotImplemented(new Runnable() { @Override public void run() { Where.notContains("k", false); } });
    }

    @Test
    public void testLogicalCombinatorsSerializeNestedConditions() {
        final Where idFilter = Where.idIn("id1", "id2");
        final Where metadataFilter = where(singletonMap("topic", "news"));
        final Where documentFilter = Where.documentContains("ai");

        Map<String, Object> expectedAnd = new LinkedHashMap<String, Object>();
        expectedAnd.put("$and", Arrays.asList(
                idFilter.toMap(),
                metadataFilter.toMap(),
                documentFilter.toMap()
        ));
        Map<String, Object> expectedOr = new LinkedHashMap<String, Object>();
        expectedOr.put("$or", Arrays.asList(
                idFilter.toMap(),
                documentFilter.toMap()
        ));

        assertEquals(expectedAnd, Where.and(idFilter, metadataFilter, documentFilter).toMap());
        assertEquals(expectedOr, Where.or(idFilter, documentFilter).toMap());
    }

    @Test
    public void testInstanceLogicalCombinatorsDelegateToStaticFactories() {
        final Where first = Where.idIn("id1");
        final Where second = Where.documentContains("ai");
        assertEquals(Where.and(first, second).toMap(), first.and(second).toMap());
        assertEquals(Where.or(first, second).toMap(), first.or(second).toMap());
    }

    @Test
    public void testIdFactoriesRejectInvalidArguments() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idIn(); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idNotIn(); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idIn((String[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idNotIn((String[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idIn("id1", " "); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idNotIn("id1", " "); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idIn("id1", null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.idNotIn("id1", null); } });
    }

    @Test
    public void testInlineDocumentFactoriesRejectInvalidArguments() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.documentContains(null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.documentNotContains(null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.documentContains(" "); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.documentNotContains("\t"); } });
    }

    @Test
    public void testLogicalCombinatorsRejectInvalidArguments() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and(); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or(); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and(new Where[]{Where.idIn("id1")}); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or(new Where[]{Where.idIn("id1")}); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and((Where[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or((Where[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and(Where.idIn("id1"), null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or(Where.idIn("id1"), null); } });
    }

    @Test
    public void testLogicalCombinatorsRejectNullToMapOutput() {
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                Where.and(Where.idIn("id1"), where(null));
            }
        });
    }

    @Test
    public void testLogicalCombinatorsDefensivelyCopyChildMaps() {
        Map<String, Object> mutableNested = singletonMap("region", "eu");
        Map<String, Object> mutableChild = singletonMap("meta", mutableNested);
        Where combined = Where.and(Where.idIn("id1"), where(mutableChild));
        int originalHashCode = combined.hashCode();

        Map<String, Object> expectedNested = singletonMap("region", "eu");
        Map<String, Object> expectedChild = singletonMap("meta", expectedNested);
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$and", Arrays.asList(
                Where.idIn("id1").toMap(),
                expectedChild
        ));

        mutableNested.put("region", "us");
        mutableChild.put("extra", "x");

        assertEquals(expected, combined.toMap());
        assertEquals(originalHashCode, combined.hashCode());
    }

    @Test
    public void testValueSemantics() {
        Where first = Where.idIn("id1", "id2");
        Where second = Where.idIn("id1", "id2");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("Where{#id={$in=[id1, id2]}}", first.toString());
    }

    private static Map<String, Object> operatorCondition(String key, String operator, Object operand) {
        Map<String, Object> op = new LinkedHashMap<String, Object>();
        op.put(operator, operand);
        return singletonMap(key, op);
    }

    private static Map<String, Object> singletonMap(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    private static Where where(final Map<String, Object> map) {
        return new Where() {
            @Override
            public Map<String, Object> toMap() {
                return map;
            }
        };
    }

    private static void assertNotImplemented(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    private static void assertIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

}
