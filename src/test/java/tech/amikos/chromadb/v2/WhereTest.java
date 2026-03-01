package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WhereTest {

    @Test
    public void testEqualityFactoriesSerializeToMap() {
        assertEquals(operatorCondition("k", "$eq", "v"), Where.eq("k", "v").toMap());
        assertEquals(operatorCondition("k", "$eq", Integer.valueOf(1)), Where.eq("k", 1).toMap());
        assertEquals(operatorCondition("k", "$eq", Float.valueOf(1.0f)), Where.eq("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$eq", Boolean.TRUE), Where.eq("k", true).toMap());

        assertEquals(operatorCondition("k", "$ne", "v"), Where.ne("k", "v").toMap());
        assertEquals(operatorCondition("k", "$ne", Integer.valueOf(1)), Where.ne("k", 1).toMap());
        assertEquals(operatorCondition("k", "$ne", Float.valueOf(1.0f)), Where.ne("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$ne", Boolean.FALSE), Where.ne("k", false).toMap());
    }

    @Test
    public void testComparisonFactoriesSerializeToMap() {
        assertEquals(operatorCondition("k", "$gt", Integer.valueOf(1)), Where.gt("k", 1).toMap());
        assertEquals(operatorCondition("k", "$gt", Float.valueOf(1.0f)), Where.gt("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$gte", Integer.valueOf(1)), Where.gte("k", 1).toMap());
        assertEquals(operatorCondition("k", "$gte", Float.valueOf(1.0f)), Where.gte("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$lt", Integer.valueOf(1)), Where.lt("k", 1).toMap());
        assertEquals(operatorCondition("k", "$lt", Float.valueOf(1.0f)), Where.lt("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$lte", Integer.valueOf(1)), Where.lte("k", 1).toMap());
        assertEquals(operatorCondition("k", "$lte", Float.valueOf(1.0f)), Where.lte("k", 1.0f).toMap());
    }

    @Test
    public void testSetFactoriesSerializeToMap() {
        assertEquals(operatorCondition("k", "$in", Arrays.asList("a", "b")), Where.in("k", "a", "b").toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Integer.valueOf(1), Integer.valueOf(2))),
                Where.in("k", 1, 2).toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Float.valueOf(1.0f), Float.valueOf(2.0f))),
                Where.in("k", 1.0f, 2.0f).toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Boolean.TRUE, Boolean.FALSE)),
                Where.in("k", true, false).toMap());

        assertEquals(operatorCondition("k", "$nin", Arrays.asList("a", "b")), Where.nin("k", "a", "b").toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Integer.valueOf(1), Integer.valueOf(2))),
                Where.nin("k", 1, 2).toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Float.valueOf(1.0f), Float.valueOf(2.0f))),
                Where.nin("k", 1.0f, 2.0f).toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Boolean.TRUE, Boolean.FALSE)),
                Where.nin("k", true, false).toMap());
    }

    @Test
    public void testSetFactoriesSerializeSingleElement() {
        assertEquals(operatorCondition("k", "$in", Arrays.asList("a")), Where.in("k", "a").toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList("a")), Where.nin("k", "a").toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Integer.valueOf(1))), Where.in("k", 1).toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Integer.valueOf(1))), Where.nin("k", 1).toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Float.valueOf(1.0f))), Where.in("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Float.valueOf(1.0f))), Where.nin("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$in", Arrays.asList(Boolean.TRUE)), Where.in("k", true).toMap());
        assertEquals(operatorCondition("k", "$nin", Arrays.asList(Boolean.TRUE)), Where.nin("k", true).toMap());
    }

    @Test
    public void testMetadataStringFactoriesPreserveWhitespaceAndAllowEmptyValues() {
        assertEquals(operatorCondition(" topic ", "$eq", "  news\t"), Where.eq(" topic ", "  news\t").toMap());
        assertEquals(operatorCondition("topic", "$eq", ""), Where.eq("topic", "").toMap());
        assertEquals(operatorCondition(" topic ", "$in", Arrays.asList("", " news ", "\tsports\t")),
                Where.in(" topic ", "", " news ", "\tsports\t").toMap());
    }

    @Test
    public void testIdFactoriesSerializeToMap() {
        Map<String, Object> expectedIn = operatorCondition("#id", "$in", Arrays.asList("id1", "id2", "id3"));
        Map<String, Object> expectedNotIn = operatorCondition("#id", "$nin", Arrays.asList("id1", "id2"));

        assertEquals(expectedIn, Where.idIn("id1", "id2", "id3").toMap());
        assertEquals(expectedNotIn, Where.idNotIn("id1", "id2").toMap());
    }

    @Test
    public void testFromMapFactoryCopiesInput() {
        Map<String, Object> nested = singletonMap("$gt", 3);
        Map<String, Object> source = singletonMap("index", nested);
        Where fromMap = Where.fromMap(source);

        nested.put("$gt", 9);
        source.put("extra", true);

        Map<String, Object> expectedNested = singletonMap("$gt", 3);
        Map<String, Object> expected = singletonMap("index", expectedNested);
        assertEquals(expected, fromMap.toMap());
    }

    @Test
    public void testFromMapFactoryRejectsNull() {
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                Where.fromMap(null);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFromMapFactoryRejectsNonStringKeys() {
        final Map raw = new LinkedHashMap();
        raw.put(Integer.valueOf(1), "bad");
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                Where.fromMap((Map<String, Object>) raw);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFromMapFactoryRejectsNestedNonStringKeys() {
        Map nestedRaw = new LinkedHashMap();
        nestedRaw.put(Integer.valueOf(1), "bad");
        final Map<String, Object> source = singletonMap("meta", nestedRaw);
        assertIllegalArgument(new Runnable() {
            @Override
            public void run() {
                Where.fromMap(source);
            }
        });
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
    public void testArrayMetadataFactoriesSerializeToMap() {
        assertEquals(operatorCondition("k", "$contains", "v"), Where.contains("k", "v").toMap());
        assertEquals(operatorCondition("k", "$contains", Integer.valueOf(1)), Where.contains("k", 1).toMap());
        assertEquals(operatorCondition("k", "$contains", Float.valueOf(1.0f)), Where.contains("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$contains", Boolean.TRUE), Where.contains("k", true).toMap());
        assertEquals(operatorCondition("k", "$not_contains", "v"), Where.notContains("k", "v").toMap());
        assertEquals(operatorCondition("k", "$not_contains", Integer.valueOf(1)), Where.notContains("k", 1).toMap());
        assertEquals(operatorCondition("k", "$not_contains", Float.valueOf(1.0f)), Where.notContains("k", 1.0f).toMap());
        assertEquals(operatorCondition("k", "$not_contains", Boolean.FALSE), Where.notContains("k", false).toMap());
    }

    @Test
    public void testLogicalCombinatorsSerializeNestedConditions() {
        final Where idFilter = Where.idIn("id1", "id2");
        final Where metadataFilter = Where.eq("topic", "news");
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
    public void testMetadataFactoriesInteroperateWithNestedIdAndDocumentFilters() {
        Where where = Where.and(
                Where.eq("topic", "news"),
                Where.or(
                        Where.idIn("id1"),
                        Where.documentContains("ai")
                )
        );

        Map<String, Object> expectedInnerOr = new LinkedHashMap<String, Object>();
        expectedInnerOr.put("$or", Arrays.asList(
                Where.idIn("id1").toMap(),
                Where.documentContains("ai").toMap()
        ));
        Map<String, Object> expected = new LinkedHashMap<String, Object>();
        expected.put("$and", Arrays.asList(
                Where.eq("topic", "news").toMap(),
                expectedInnerOr
        ));

        assertEquals(expected, where.toMap());
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
    public void testMetadataFactoriesRejectInvalidArguments() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq(null, "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("   ", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("k", (String) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.ne(null, "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.ne("", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.ne("   ", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.ne("k", null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.gt(null, 1); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.gte("", 1); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.lt(null, 1.0f); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.lte("", 1.0f); } });

        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in(null, "a"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("", "a"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", new String[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", (String[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", "a", null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", (int[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", new int[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", (float[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", new float[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", (boolean[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", new boolean[0]); } });

        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin(null, "a"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("", "a"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", new String[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", (String[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", "a", null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", (int[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", new int[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", (float[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", new float[0]); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", (boolean[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", new boolean[0]); } });
    }

    @Test
    public void testMetadataFactoriesRejectReservedKeyPrefixes() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("#id", "x"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("$and", "x"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("#document", "x"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("$contains", "x"); } });
    }

    @Test
    public void testMetadataFactoryToMapIsUnmodifiable() {
        Map<String, Object> map = Where.eq("k", "v").toMap();
        try {
            map.put("new", "x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) map.get("k");
        try {
            inner.put("$eq", "x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testFloatFactoriesRejectNonFiniteValues() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.eq("k", Float.NaN); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.ne("k", Float.POSITIVE_INFINITY); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.gt("k", Float.NEGATIVE_INFINITY); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("k", Float.NaN); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.in("k", 1.0f, Float.NaN); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.nin("k", 1.0f, Float.POSITIVE_INFINITY); } });
    }

    @Test
    public void testMetadataContainsStringFactoriesRejectNullOrEmptyValue() {
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains(null, "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("   ", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.notContains(null, "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.notContains("", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.notContains("   ", "v"); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("k", (String) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.notContains("k", (String) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.contains("k", ""); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.notContains("k", ""); } });
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
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and((Where[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or((Where[]) null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.and(Where.idIn("id1"), null); } });
        assertIllegalArgument(new Runnable() { @Override public void run() { Where.or(Where.idIn("id1"), null); } });
    }

    @Test
    public void testLogicalCombinatorsAcceptSingleClause() {
        assertEquals(singletonMap("$and", Arrays.asList(Where.idIn("id1").toMap())),
                Where.and(new Where[]{Where.idIn("id1")}).toMap());
        assertEquals(singletonMap("$or", Arrays.asList(Where.idIn("id1").toMap())),
                Where.or(new Where[]{Where.idIn("id1")}).toMap());
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

    private static void assertIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

}
