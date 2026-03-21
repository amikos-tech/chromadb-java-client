package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // -----------------------------------------------------------------------
    // GetResultImpl.rows() wiring tests
    // -----------------------------------------------------------------------

    private ChromaDtos.GetResponse buildGetResponse(int n, boolean includeDocuments,
                                                     boolean includeMetadatas,
                                                     boolean includeEmbeddings,
                                                     boolean includeUris) {
        ChromaDtos.GetResponse dto = new ChromaDtos.GetResponse();
        dto.ids = new ArrayList<String>();
        dto.documents = includeDocuments ? new ArrayList<String>() : null;
        dto.metadatas = includeMetadatas ? new ArrayList<Map<String, Object>>() : null;
        dto.embeddings = includeEmbeddings ? new ArrayList<List<Float>>() : null;
        dto.uris = includeUris ? new ArrayList<String>() : null;

        for (int i = 0; i < n; i++) {
            dto.ids.add("id" + i);
            if (includeDocuments) dto.documents.add("doc" + i);
            if (includeMetadatas) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("index", i);
                dto.metadatas.add(m);
            }
            if (includeEmbeddings) {
                dto.embeddings.add(Arrays.asList(Float.valueOf((i + 1) * 0.1f), Float.valueOf(0.2f), Float.valueOf(0.3f)));
            }
            if (includeUris) dto.uris.add("uri" + i);
        }
        return dto;
    }

    @Test
    public void testGetResultRows() {
        ChromaDtos.GetResponse dto = buildGetResponse(3, true, true, true, true);
        GetResultImpl result = GetResultImpl.from(dto);

        ResultGroup<ResultRow> rows = result.rows();

        assertEquals(3, rows.size());
        assertEquals("id0", rows.get(0).getId());
        assertEquals("doc0", rows.get(0).getDocument());
        assertNotNull(rows.get(0).getMetadata());
        assertEquals(Integer.valueOf(0), rows.get(0).getMetadata().get("index"));
        assertNotNull(rows.get(0).getEmbedding());
        assertEquals(3, rows.get(0).getEmbedding().length);
        assertEquals("uri0", rows.get(0).getUri());

        assertEquals("id1", rows.get(1).getId());
        assertEquals("id2", rows.get(2).getId());
    }

    @Test
    public void testGetResultRowsNullFields() {
        // documents, metadatas, embeddings, uris all null (not included)
        ChromaDtos.GetResponse dto = buildGetResponse(2, false, false, false, false);
        GetResultImpl result = GetResultImpl.from(dto);

        ResultGroup<ResultRow> rows = result.rows();

        assertEquals(2, rows.size());
        assertEquals("id0", rows.get(0).getId());
        assertNull(rows.get(0).getDocument());
        assertNull(rows.get(0).getMetadata());
        assertNull(rows.get(0).getEmbedding());
        assertNull(rows.get(0).getUri());
    }

    // -----------------------------------------------------------------------
    // QueryResultImpl.rows(int), groupCount(), stream() wiring tests
    // -----------------------------------------------------------------------

    private ChromaDtos.QueryResponse buildQueryResponse(int queryCount, int nResults,
                                                         boolean includeDocuments,
                                                         boolean includeMetadatas,
                                                         boolean includeDistances) {
        ChromaDtos.QueryResponse dto = new ChromaDtos.QueryResponse();
        dto.ids = new ArrayList<List<String>>();
        dto.documents = includeDocuments ? new ArrayList<List<String>>() : null;
        dto.metadatas = includeMetadatas ? new ArrayList<List<Map<String, Object>>>() : null;
        dto.distances = includeDistances ? new ArrayList<List<Float>>() : null;
        dto.embeddings = null;
        dto.uris = null;

        for (int q = 0; q < queryCount; q++) {
            List<String> innerIds = new ArrayList<String>();
            List<String> innerDocs = includeDocuments ? new ArrayList<String>() : null;
            List<Map<String, Object>> innerMetas = includeMetadatas ? new ArrayList<Map<String, Object>>() : null;
            List<Float> innerDists = includeDistances ? new ArrayList<Float>() : null;

            for (int i = 0; i < nResults; i++) {
                innerIds.add("q" + q + "-id" + i);
                if (includeDocuments) innerDocs.add("q" + q + "-doc" + i);
                if (includeMetadatas) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("qi", q * nResults + i);
                    innerMetas.add(m);
                }
                if (includeDistances) innerDists.add(Float.valueOf(q * 0.1f + i * 0.01f));
            }
            dto.ids.add(innerIds);
            if (includeDocuments) dto.documents.add(innerDocs);
            if (includeMetadatas) dto.metadatas.add(innerMetas);
            if (includeDistances) dto.distances.add(innerDists);
        }
        return dto;
    }

    @Test
    public void testQueryResultRowsByIndex() {
        ChromaDtos.QueryResponse dto = buildQueryResponse(2, 3, true, true, true);
        QueryResultImpl result = QueryResultImpl.from(dto);

        ResultGroup<QueryResultRow> group0 = result.rows(0);
        assertEquals(3, group0.size());
        assertEquals("q0-id0", group0.get(0).getId());
        assertEquals("q0-doc0", group0.get(0).getDocument());
        assertNotNull(group0.get(0).getMetadata());
        assertNotNull(group0.get(0).getDistance());

        ResultGroup<QueryResultRow> group1 = result.rows(1);
        assertEquals(3, group1.size());
        assertEquals("q1-id0", group1.get(0).getId());
        assertEquals("q1-doc0", group1.get(0).getDocument());
    }

    @Test
    public void testQueryResultGroupCount() {
        ChromaDtos.QueryResponse dto = buildQueryResponse(3, 2, false, false, false);
        QueryResultImpl result = QueryResultImpl.from(dto);

        assertEquals(3, result.groupCount());
    }

    @Test
    public void testQueryResultStreamFlatMap() {
        // 2 query inputs, 3 results each → 6 total rows
        ChromaDtos.QueryResponse dto = buildQueryResponse(2, 3, false, false, false);
        QueryResultImpl result = QueryResultImpl.from(dto);

        long totalRows = result.stream()
                .flatMap(ResultGroup::stream)
                .count();
        assertEquals(6L, totalRows);
    }

    @Test
    public void testQueryResultRowDistance() {
        ChromaDtos.QueryResponse dto = buildQueryResponse(1, 2, false, false, true);
        QueryResultImpl result = QueryResultImpl.from(dto);

        ResultGroup<QueryResultRow> group = result.rows(0);
        assertNotNull(group.get(0).getDistance());
        assertNotNull(group.get(1).getDistance());
        // first distance should be 0.0f (q=0, i=0 → 0*0.1+0*0.01)
        assertEquals(0.0f, group.get(0).getDistance(), 0.001f);
        assertEquals(0.01f, group.get(1).getDistance(), 0.001f);
    }

    @Test
    public void testQueryResultRowsNullDistances() {
        ChromaDtos.QueryResponse dto = buildQueryResponse(1, 2, true, false, false);
        QueryResultImpl result = QueryResultImpl.from(dto);

        ResultGroup<QueryResultRow> group = result.rows(0);
        assertNull(group.get(0).getDistance());
        assertNull(group.get(1).getDistance());
    }

    // -----------------------------------------------------------------------
    // Caching: rows() returns same reference on repeated calls
    // -----------------------------------------------------------------------

    @Test
    public void testGetResultRowsCachesResult() {
        ChromaDtos.GetResponse dto = buildGetResponse(3, true, true, true, true);
        GetResultImpl result = GetResultImpl.from(dto);

        ResultGroup<ResultRow> first = result.rows();
        ResultGroup<ResultRow> second = result.rows();
        assertSame(first, second);
    }

    @Test
    public void testQueryResultRowsCachesPerIndex() {
        ChromaDtos.QueryResponse dto = buildQueryResponse(2, 3, true, false, true);
        QueryResultImpl result = QueryResultImpl.from(dto);

        ResultGroup<QueryResultRow> first0 = result.rows(0);
        ResultGroup<QueryResultRow> second0 = result.rows(0);
        assertSame(first0, second0);

        ResultGroup<QueryResultRow> first1 = result.rows(1);
        ResultGroup<QueryResultRow> second1 = result.rows(1);
        assertSame(first1, second1);

        assertNotSame(first0, first1);
    }

    // -----------------------------------------------------------------------
    // GetResult.stream()
    // -----------------------------------------------------------------------

    @Test
    public void testGetResultStream() {
        ChromaDtos.GetResponse dto = buildGetResponse(3, true, false, false, false);
        GetResultImpl result = GetResultImpl.from(dto);

        List<String> ids = result.stream()
                .map(ResultRow::getId)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("id0", "id1", "id2"), ids);
    }

    // -----------------------------------------------------------------------
    // ResultRowImpl: equals / hashCode / toString
    // -----------------------------------------------------------------------

    @Test
    public void testResultRowEqualsAndHashCode() {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("key", "value");
        float[] emb = new float[]{0.1f, 0.2f};

        ResultRowImpl a = new ResultRowImpl("id-1", "doc", meta, emb, "uri");
        ResultRowImpl b = new ResultRowImpl("id-1", "doc", meta, emb, "uri");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testResultRowNotEqualsDifferentId() {
        ResultRowImpl a = new ResultRowImpl("id-1", null, null, null, null);
        ResultRowImpl b = new ResultRowImpl("id-2", null, null, null, null);
        assertNotEquals(a, b);
    }

    @Test
    public void testResultRowNotEqualsDifferentEmbedding() {
        ResultRowImpl a = new ResultRowImpl("id-1", null, null, new float[]{1.0f}, null);
        ResultRowImpl b = new ResultRowImpl("id-1", null, null, new float[]{2.0f}, null);
        assertNotEquals(a, b);
    }

    @Test
    public void testResultRowEqualsNull() {
        ResultRowImpl a = new ResultRowImpl("id-1", null, null, null, null);
        assertNotEquals(a, null);
    }

    @Test
    public void testResultRowToString() {
        ResultRowImpl row = new ResultRowImpl("id-1", "doc", null, null, null);
        String str = row.toString();
        assertTrue(str.contains("id-1"));
        assertTrue(str.contains("doc"));
        assertTrue(str.startsWith("ResultRow{"));
    }

    // -----------------------------------------------------------------------
    // QueryResultRowImpl: equals / hashCode / toString
    // -----------------------------------------------------------------------

    @Test
    public void testQueryResultRowEqualsAndHashCode() {
        QueryResultRowImpl a = new QueryResultRowImpl("id-1", "doc", null, null, null, 0.5f);
        QueryResultRowImpl b = new QueryResultRowImpl("id-1", "doc", null, null, null, 0.5f);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testQueryResultRowNotEqualsDifferentDistance() {
        QueryResultRowImpl a = new QueryResultRowImpl("id-1", "doc", null, null, null, 0.5f);
        QueryResultRowImpl b = new QueryResultRowImpl("id-1", "doc", null, null, null, 0.9f);
        assertNotEquals(a, b);
    }

    @Test
    public void testQueryResultRowToString() {
        QueryResultRowImpl row = new QueryResultRowImpl("id-1", "doc", null, null, null, 0.42f);
        String str = row.toString();
        assertTrue(str.contains("id-1"));
        assertTrue(str.contains("0.42"));
        assertTrue(str.startsWith("QueryResultRow{"));
    }

    // -----------------------------------------------------------------------
    // ResultGroupImpl: equals / hashCode / toString
    // -----------------------------------------------------------------------

    @Test
    public void testResultGroupEqualsAndHashCode() {
        ResultRowImpl row = new ResultRowImpl("id-0", "doc", null, null, null);
        ResultGroupImpl<ResultRow> a = new ResultGroupImpl<ResultRow>(Arrays.<ResultRow>asList(row));
        ResultGroupImpl<ResultRow> b = new ResultGroupImpl<ResultRow>(Arrays.<ResultRow>asList(row));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testResultGroupNotEquals() {
        ResultRowImpl row0 = new ResultRowImpl("id-0", null, null, null, null);
        ResultRowImpl row1 = new ResultRowImpl("id-1", null, null, null, null);
        ResultGroupImpl<ResultRow> a = new ResultGroupImpl<ResultRow>(Arrays.<ResultRow>asList(row0));
        ResultGroupImpl<ResultRow> b = new ResultGroupImpl<ResultRow>(Arrays.<ResultRow>asList(row1));
        assertNotEquals(a, b);
    }

    @Test
    public void testResultGroupToString() {
        ResultRowImpl row = new ResultRowImpl("id-0", null, null, null, null);
        ResultGroupImpl<ResultRow> group = new ResultGroupImpl<ResultRow>(
                Arrays.<ResultRow>asList(row));
        String str = group.toString();
        assertTrue(str.startsWith("ResultGroup["));
        assertTrue(str.contains("id-0"));
    }
}
