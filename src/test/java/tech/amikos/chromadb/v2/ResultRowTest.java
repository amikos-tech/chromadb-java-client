package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ResultRowTest {

    // -----------------------------------------------------------------------
    // ResultRowImpl: null fields
    // -----------------------------------------------------------------------

    @Test
    public void testResultRowFieldsNullWhenNotIncluded() {
        ResultRowImpl row = new ResultRowImpl(null, null, null, null, null);
        assertNull(row.getId());
        assertNull(row.getDocument());
        assertNull(row.getMetadata());
        assertNull(row.getEmbedding());
        assertNull(row.getUri());
    }

    // -----------------------------------------------------------------------
    // ResultRowImpl: all fields populated
    // -----------------------------------------------------------------------

    @Test
    public void testResultRowStoresAndReturnsAllFields() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("color", "blue");
        metadata.put("count", Integer.valueOf(3));

        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        ResultRowImpl row = new ResultRowImpl("id-1", "Hello world", metadata, embedding, "s3://bucket/file");

        assertEquals("id-1", row.getId());
        assertEquals("Hello world", row.getDocument());
        assertEquals("s3://bucket/file", row.getUri());

        Map<String, Object> returnedMeta = row.getMetadata();
        assertEquals("blue", returnedMeta.get("color"));
        assertEquals(Integer.valueOf(3), returnedMeta.get("count"));

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, row.getEmbedding(), 0.0f);
    }

    // -----------------------------------------------------------------------
    // ResultRowImpl: embedding defensive copy
    // -----------------------------------------------------------------------

    @Test
    public void testEmbeddingDefensiveCopy() {
        float[] original = new float[]{1.0f, 2.0f, 3.0f};
        ResultRowImpl row = new ResultRowImpl("id-1", null, null, original, null);

        // Mutate the array returned from getEmbedding()
        float[] returned1 = row.getEmbedding();
        returned1[0] = 999.0f;

        // A second call should still return the original values
        float[] returned2 = row.getEmbedding();
        assertEquals(1.0f, returned2[0], 0.0f);
        assertEquals(2.0f, returned2[1], 0.0f);
        assertEquals(3.0f, returned2[2], 0.0f);

        // Mutating the constructor input should also not affect stored value
        original[0] = 888.0f;
        float[] returned3 = row.getEmbedding();
        assertEquals(1.0f, returned3[0], 0.0f);
    }

    @Test
    public void testEmbeddingNullReturnedAsNull() {
        ResultRowImpl row = new ResultRowImpl("id-1", null, null, null, null);
        assertNull(row.getEmbedding());
    }

    // -----------------------------------------------------------------------
    // ResultRowImpl: metadata unmodifiable
    // -----------------------------------------------------------------------

    @Test
    public void testMetadataUnmodifiable() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("key", "value");
        ResultRowImpl row = new ResultRowImpl("id-1", null, metadata, null, null);

        Map<String, Object> returned = row.getMetadata();
        try {
            returned.put("new-key", "new-value");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testMetadataConstructorMutationDoesNotAffectStoredMap() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("key", "value");
        ResultRowImpl row = new ResultRowImpl("id-1", null, metadata, null, null);

        // Mutate the original map after construction
        metadata.put("extra", "extra-value");

        // Stored map should not contain the mutation
        assertNull(row.getMetadata().get("extra"));
        assertEquals(1, row.getMetadata().size());
    }

    @Test
    public void testMetadataNullReturnedAsNull() {
        ResultRowImpl row = new ResultRowImpl("id-1", null, null, null, null);
        assertNull(row.getMetadata());
    }

    // -----------------------------------------------------------------------
    // QueryResultRowImpl: distance
    // -----------------------------------------------------------------------

    @Test
    public void testQueryResultRowDistanceReturnsBoxedFloat() {
        QueryResultRowImpl row = new QueryResultRowImpl("id-1", "doc", null, null, null, Float.valueOf(0.42f));
        assertEquals(Float.valueOf(0.42f), row.getDistance());
    }

    @Test
    public void testQueryResultRowDistanceNullWhenNotIncluded() {
        QueryResultRowImpl row = new QueryResultRowImpl("id-1", "doc", null, null, null, null);
        assertNull(row.getDistance());
    }

    // -----------------------------------------------------------------------
    // QueryResultRowImpl: inherits ResultRow methods
    // -----------------------------------------------------------------------

    @Test
    public void testQueryResultRowInheritsAllResultRowMethods() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("topic", "ai");

        float[] embedding = new float[]{0.5f, 0.6f};
        QueryResultRowImpl row = new QueryResultRowImpl(
                "id-q", "query doc", metadata, embedding, "s3://q", Float.valueOf(1.5f)
        );

        assertEquals("id-q", row.getId());
        assertEquals("query doc", row.getDocument());
        assertEquals("s3://q", row.getUri());
        assertEquals("ai", row.getMetadata().get("topic"));
        assertArrayEquals(new float[]{0.5f, 0.6f}, row.getEmbedding(), 0.0f);
        assertEquals(Float.valueOf(1.5f), row.getDistance());
    }

    @Test
    public void testQueryResultRowEmbeddingDefensiveCopy() {
        float[] embedding = new float[]{1.0f, 2.0f};
        QueryResultRowImpl row = new QueryResultRowImpl("id-1", null, null, embedding, null, null);

        float[] returned1 = row.getEmbedding();
        returned1[0] = 999.0f;

        float[] returned2 = row.getEmbedding();
        assertEquals(1.0f, returned2[0], 0.0f);
    }

    @Test
    public void testQueryResultRowMetadataUnmodifiable() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("key", "value");
        QueryResultRowImpl row = new QueryResultRowImpl("id-1", null, metadata, null, null, null);

        try {
            row.getMetadata().put("new", "x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    // -----------------------------------------------------------------------
    // ResultGroupImpl: basic operations
    // -----------------------------------------------------------------------

    @Test
    public void testResultGroupGetReturnsFirstElement() {
        ResultRowImpl row0 = new ResultRowImpl("id-0", "doc0", null, null, null);
        ResultRowImpl row1 = new ResultRowImpl("id-1", "doc1", null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row0, row1)
        );

        assertEquals("id-0", group.get(0).getId());
        assertEquals("id-1", group.get(1).getId());
    }

    @Test
    public void testResultGroupGetThrowsOnBadIndex() {
        ResultRowImpl row = new ResultRowImpl("id-0", "doc", null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row)
        );

        try {
            group.get(1);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }

        try {
            group.get(-1);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void testResultGroupGetThrowsOnEmptyGroup() {
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Collections.<ResultRow>emptyList()
        );
        try {
            group.get(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void testResultGroupSize() {
        ResultGroupImpl<ResultRow> empty = new ResultGroupImpl<ResultRow>(
                Collections.<ResultRow>emptyList()
        );
        assertEquals(0, empty.size());

        ResultRowImpl row = new ResultRowImpl("id-0", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row, row, row)
        );
        assertEquals(3, group.size());
    }

    @Test
    public void testResultGroupIsEmpty() {
        ResultGroupImpl<ResultRow> empty = new ResultGroupImpl<ResultRow>(
                Collections.<ResultRow>emptyList()
        );
        assertTrue(empty.isEmpty());

        ResultRowImpl row = new ResultRowImpl("id-0", null, null, null, null);
        ResultGroupImpl<ResultRow> nonEmpty = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row)
        );
        assertFalse(nonEmpty.isEmpty());
    }

    @Test
    public void testResultGroupStreamHasCorrectSizeAndElements() {
        ResultRowImpl row0 = new ResultRowImpl("id-0", null, null, null, null);
        ResultRowImpl row1 = new ResultRowImpl("id-1", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row0, row1)
        );

        List<ResultRow> collected = group.stream().collect(java.util.stream.Collectors.toList());
        assertEquals(2, collected.size());
        assertEquals("id-0", collected.get(0).getId());
        assertEquals("id-1", collected.get(1).getId());
    }

    @Test
    public void testResultGroupToListIsUnmodifiable() {
        ResultRowImpl row = new ResultRowImpl("id-0", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row)
        );

        List<ResultRow> list = group.toList();
        assertEquals(1, list.size());

        try {
            list.add(row);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testResultGroupImplementsIterable() {
        ResultRowImpl row0 = new ResultRowImpl("id-0", null, null, null, null);
        ResultRowImpl row1 = new ResultRowImpl("id-1", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row0, row1)
        );

        int count = 0;
        for (ResultRow row : group) {
            assertNotNull(row.getId());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testResultGroupIterator() {
        ResultRowImpl row0 = new ResultRowImpl("id-0", null, null, null, null);
        ResultRowImpl row1 = new ResultRowImpl("id-1", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row0, row1)
        );

        Iterator<ResultRow> it = group.iterator();
        assertTrue(it.hasNext());
        assertEquals("id-0", it.next().getId());
        assertTrue(it.hasNext());
        assertEquals("id-1", it.next().getId());
        assertFalse(it.hasNext());
    }

    // -----------------------------------------------------------------------
    // QueryResultRowImpl in ResultGroup
    // -----------------------------------------------------------------------

    @Test
    public void testResultGroupWithQueryResultRows() {
        QueryResultRowImpl qr0 = new QueryResultRowImpl("q-0", "doc0", null, null, null, 0.1f);
        QueryResultRowImpl qr1 = new QueryResultRowImpl("q-1", "doc1", null, null, null, 0.9f);
        ResultGroupImpl<QueryResultRow> group = new ResultGroupImpl<QueryResultRow>(
                Arrays.<QueryResultRow>asList(qr0, qr1)
        );

        assertEquals(2, group.size());
        assertEquals("q-0", group.get(0).getId());
        assertEquals(Float.valueOf(0.9f), group.get(1).getDistance());
    }
}
