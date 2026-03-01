package tech.amikos.chromadb.v2;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RecordOperationsIntegrationTest extends AbstractChromaIntegrationTest {

    private static final int DIM = 3;

    private Collection collection;

    private static Where where(Map<String, Object> map) {
        final Map<String, Object> copy = new LinkedHashMap<String, Object>(map);
        return new Where() {
            @Override
            public Map<String, Object> toMap() {
                return copy;
            }
        };
    }

    private static WhereDocument whereDocument(Map<String, Object> map) {
        final Map<String, Object> copy = new LinkedHashMap<String, Object>(map);
        return new WhereDocument() {
            @Override
            public Map<String, Object> toMap() {
                return copy;
            }
        };
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        collection = client.createCollection("records_col");
    }

    private void addSampleRecords(int n) {
        List<String> ids = new ArrayList<String>();
        List<float[]> embs = new ArrayList<float[]>();
        List<String> docs = new ArrayList<String>();
        List<Map<String, Object>> metas = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < n; i++) {
            ids.add("id" + i);
            float[] v = new float[DIM];
            for (int d = 0; d < DIM; d++) {
                v[d] = (i + 1) * 0.1f + d * 0.01f;
            }
            embs.add(v);
            docs.add("document " + i);
            Map<String, Object> meta = new HashMap<String, Object>();
            meta.put("index", i);
            meta.put("category", i % 2 == 0 ? "even" : "odd");
            metas.add(meta);
        }

        collection.add()
                .ids(ids)
                .embeddings(embs)
                .documents(docs)
                .metadatas(metas)
                .execute();
    }

    // --- add: with embeddings only ---

    @Test
    public void testAddWithEmbeddings() {
        collection.add()
                .ids("a1", "a2")
                .embeddings(new float[]{1, 0, 0}, new float[]{0, 1, 0})
                .execute();

        assertEquals(2, collection.count());
    }

    // --- add: with documents + embeddings ---

    @Test
    public void testAddWithDocumentsAndEmbeddings() {
        collection.add()
                .ids("d1", "d2")
                .embeddings(new float[]{1, 0, 0}, new float[]{0, 1, 0})
                .documents("hello", "world")
                .execute();

        GetResult result = collection.get()
                .ids("d1")
                .include(Include.DOCUMENTS)
                .execute();

        assertEquals(1, result.getIds().size());
        assertEquals("hello", result.getDocuments().get(0));
    }

    // --- add: with metadatas ---

    @Test
    public void testAddWithMetadatas() {
        Map<String, Object> m1 = new HashMap<String, Object>();
        m1.put("color", "red");
        Map<String, Object> m2 = new HashMap<String, Object>();
        m2.put("color", "blue");

        collection.add()
                .ids("m1", "m2")
                .embeddings(new float[]{1, 0, 0}, new float[]{0, 1, 0})
                .metadatas(Arrays.asList(m1, m2))
                .execute();

        GetResult result = collection.get()
                .ids("m1")
                .include(Include.METADATAS)
                .execute();

        assertEquals("red", result.getMetadatas().get(0).get("color"));
    }

    // --- get: by IDs ---

    @Test
    public void testGetByIds() {
        addSampleRecords(5);

        GetResult result = collection.get()
                .ids("id0", "id2")
                .execute();

        assertEquals(2, result.getIds().size());
        assertTrue(result.getIds().contains("id0"));
        assertTrue(result.getIds().contains("id2"));
    }

    @Test
    public void testInlineDocumentWhereFiltersAreRejectedByLocalChroma() {
        addSampleRecords(5);

        assertLocalWhereDocumentInlineRejected(new Runnable() {
            @Override
            public void run() {
                collection.get()
                        .where(Where.documentContains("document 4"))
                        .execute();
            }
        });
        assertLocalWhereDocumentInlineRejected(new Runnable() {
            @Override
            public void run() {
                collection.query()
                        .queryEmbeddings(new float[]{0.1f, 0.11f, 0.12f})
                        .nResults(5)
                        .where(Where.documentNotContains("document 3"))
                        .execute();
            }
        });

        // Ensure the legacy whereDocument path still works on local Chroma.
        Map<String, Object> whereDocumentMap = new LinkedHashMap<String, Object>();
        whereDocumentMap.put("$contains", "document 4");
        GetResult getResult = collection.get()
                .whereDocument(whereDocument(whereDocumentMap))
                .execute();
        assertEquals(1, getResult.getIds().size());
        assertEquals("id4", getResult.getIds().get(0));
    }

    @Test
    public void testGetAcceptsWhereIdInFilterWithoutClientError() {
        addSampleRecords(5);

        GetResult result = collection.get()
                .where(Where.idIn("id1", "id3"))
                .execute();

        // Local Chroma ID-filter semantics may vary by server version; this test pins request acceptance.
        assertNotNull(result.getIds());
    }

    // --- get: all records ---

    @Test
    public void testGetAllRecords() {
        addSampleRecords(3);

        GetResult result = collection.get().execute();
        assertEquals(3, result.getIds().size());
    }

    // --- get: with limit/offset ---

    @Test
    public void testGetWithLimitOffset() {
        addSampleRecords(5);

        GetResult result = collection.get()
                .limit(2)
                .offset(0)
                .execute();

        assertEquals(2, result.getIds().size());
    }

    // --- get: with Include.EMBEDDINGS ---

    @Test
    public void testGetWithIncludeEmbeddings() {
        collection.add()
                .ids("e1")
                .embeddings(new float[]{1.0f, 2.0f, 3.0f})
                .execute();

        GetResult result = collection.get()
                .ids("e1")
                .include(Include.EMBEDDINGS)
                .execute();

        assertNotNull(result.getEmbeddings());
        assertEquals(1, result.getEmbeddings().size());
        assertEquals(DIM, result.getEmbeddings().get(0).length);
        assertEquals(1.0f, result.getEmbeddings().get(0)[0], 0.001f);
    }

    // --- query: nearest neighbor ---

    @Test
    public void testQueryNearestNeighbor() {
        collection.add()
                .ids("q1", "q2", "q3")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f})
                .execute();

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(2)
                .include(Include.DISTANCES)
                .execute();

        assertNotNull(result.getIds());
        assertEquals(1, result.getIds().size()); // one query → one result list
        assertEquals(2, result.getIds().get(0).size());

        // first result should be the closest match
        assertEquals("q1", result.getIds().get(0).get(0));

        // distances should be non-null and ordered ascending
        assertNotNull(result.getDistances());
        float firstDist = result.getDistances().get(0).get(0);
        float secondDist = result.getDistances().get(0).get(1);
        assertTrue("distances should be ascending, got " + firstDist + " >= " + secondDist,
                firstDist <= secondDist);
    }

    // --- query: multiple query embeddings ---

    @Test
    public void testQueryMultipleEmbeddings() {
        collection.add()
                .ids("mq1", "mq2")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f})
                .execute();

        QueryResult result = collection.query()
                .queryEmbeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f})
                .nResults(1)
                .execute();

        assertEquals(2, result.getIds().size()); // two queries → two result lists
        assertEquals("mq1", result.getIds().get(0).get(0));
        assertEquals("mq2", result.getIds().get(1).get(0));
    }

    @Test
    public void testQueryAcceptsWhereIdNotInFilterWithoutClientError() {
        addSampleRecords(5);

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{0.1f, 0.11f, 0.12f})
                .nResults(3)
                .where(Where.idNotIn("id4"))
                .execute();

        // Local Chroma ID-filter semantics may vary by server version; this test pins request acceptance.
        assertNotNull(result.getIds());
        assertEquals(1, result.getIds().size());
    }

    // --- query: all include fields ---

    @Test
    public void testQueryWithAllIncludeFields() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("key", "val");

        collection.add()
                .ids("ai1")
                .embeddings(new float[]{1.0f, 0.0f, 0.0f})
                .documents("all includes doc")
                .metadatas(Arrays.asList(meta))
                .execute();

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(1)
                .include(Include.EMBEDDINGS, Include.DOCUMENTS, Include.METADATAS, Include.DISTANCES)
                .execute();

        assertEquals(1, result.getIds().get(0).size());
        assertNotNull(result.getEmbeddings());
        assertNotNull(result.getDocuments());
        assertNotNull(result.getMetadatas());
        assertNotNull(result.getDistances());

        assertEquals("all includes doc", result.getDocuments().get(0).get(0));
        assertEquals("val", result.getMetadatas().get(0).get(0).get("key"));
    }

    // --- update: document ---

    @Test
    public void testUpdateDocument() {
        collection.add()
                .ids("u1")
                .embeddings(new float[]{1, 0, 0})
                .documents("original")
                .execute();

        collection.update()
                .ids("u1")
                .documents("updated")
                .execute();

        GetResult result = collection.get()
                .ids("u1")
                .include(Include.DOCUMENTS)
                .execute();

        assertEquals("updated", result.getDocuments().get(0));
    }

    // --- update: embedding ---

    @Test
    public void testUpdateEmbedding() {
        collection.add()
                .ids("ue1")
                .embeddings(new float[]{1, 0, 0})
                .execute();

        collection.update()
                .ids("ue1")
                .embeddings(new float[]{0, 1, 0})
                .execute();

        GetResult result = collection.get()
                .ids("ue1")
                .include(Include.EMBEDDINGS)
                .execute();

        float[] emb = result.getEmbeddings().get(0);
        assertEquals(0.0f, emb[0], 0.001f);
        assertEquals(1.0f, emb[1], 0.001f);
    }

    // --- upsert: insert new ---

    @Test
    public void testUpsertInsertNew() {
        assertEquals(0, collection.count());

        collection.upsert()
                .ids("up1")
                .embeddings(new float[]{1, 0, 0})
                .documents("upserted")
                .execute();

        assertEquals(1, collection.count());

        GetResult result = collection.get()
                .ids("up1")
                .include(Include.DOCUMENTS)
                .execute();
        assertEquals("upserted", result.getDocuments().get(0));
    }

    // --- upsert: update existing ---

    @Test
    public void testUpsertUpdateExisting() {
        collection.add()
                .ids("up2")
                .embeddings(new float[]{1, 0, 0})
                .documents("original")
                .execute();

        collection.upsert()
                .ids("up2")
                .embeddings(new float[]{0, 1, 0})
                .documents("replaced")
                .execute();

        assertEquals(1, collection.count());

        GetResult result = collection.get()
                .ids("up2")
                .include(Include.DOCUMENTS)
                .execute();
        assertEquals("replaced", result.getDocuments().get(0));
    }

    // --- delete: by IDs ---

    @Test
    public void testDeleteByIds() {
        addSampleRecords(3);
        assertEquals(3, collection.count());

        collection.delete()
                .ids("id0", "id1")
                .execute();

        assertEquals(1, collection.count());

        GetResult result = collection.get().execute();
        assertEquals(1, result.getIds().size());
        assertEquals("id2", result.getIds().get(0));
    }

    @Test
    public void testDeleteByWhereAndWhereDocumentFilters() {
        addSampleRecords(5);
        assertEquals(5, collection.count());

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("category", "odd");
        Map<String, Object> whereDocumentMap = new LinkedHashMap<String, Object>();
        whereDocumentMap.put("$contains", "document");

        collection.delete()
                .where(where(whereMap))
                .whereDocument(whereDocument(whereDocumentMap))
                .execute();

        assertEquals(3, collection.count());
        GetResult result = collection.get().execute();
        assertEquals(3, result.getIds().size());
        assertTrue(result.getIds().contains("id0"));
        assertTrue(result.getIds().contains("id2"));
        assertTrue(result.getIds().contains("id4"));
    }

    @Test
    public void testDeleteRejectsWhereIdNotInFilter() {
        addSampleRecords(5);

        Map<String, Object> categoryOdd = new LinkedHashMap<String, Object>();
        categoryOdd.put("category", "odd");

        try {
            collection.delete()
                    .where(Where.and(
                            Where.idNotIn("id1"),
                            where(categoryOdd)
                    ))
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("#id"));
        }
    }

    @Test
    public void testDeleteRejectsWhereIdInFilter() {
        addSampleRecords(5);

        try {
            collection.delete()
                    .where(Where.idIn("id1", "id2"))
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("#id"));
        }
    }

    private static void assertLocalWhereDocumentInlineRejected(Runnable action) {
        try {
            action.run();
            fail("Expected ChromaBadRequestException");
        } catch (ChromaBadRequestException e) {
            assertTrue(e.getMessage().contains("InvalidArgumentError"));
        }
    }

}
