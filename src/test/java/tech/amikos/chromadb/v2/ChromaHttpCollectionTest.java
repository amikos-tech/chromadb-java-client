package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class ChromaHttpCollectionTest {

    private static final String COLLECTIONS_PATH = "/api/v2/tenants/default_tenant/databases/default_database/collections";

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private Client client;
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

    @Before
    public void setUp() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        client = ChromaClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        collection = client.getOrCreateCollection("test_col");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // --- count ---

    @Test
    public void testCount() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/count"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("42")));

        assertEquals(42, collection.count());
    }

    // --- modifyName ---

    @Test
    public void testModifyName() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_name", equalTo("renamed")))
                .willReturn(aResponse().withStatus(200)));

        collection.modifyName("renamed");
        assertEquals("renamed", collection.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testModifyNameRejectsNull() {
        collection.modifyName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModifyNameRejectsBlank() {
        collection.modifyName("   ");
    }

    // --- modifyMetadata ---

    @Test
    public void testModifyMetadata() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_metadata.key", equalTo("val")))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("key", "val");
        collection.modifyMetadata(meta);
        assertEquals("val", collection.getMetadata().get("key"));
    }

    @Test
    public void testModifyMetadataMergesWithExistingMetadata() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"metadata\":{\"existing\":\"old\"}}")));
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_metadata.new_key", equalTo("new_val")))
                .willReturn(aResponse().withStatus(200)));

        Collection col = client.getCollection("test_col");
        Map<String, Object> update = new HashMap<String, Object>();
        update.put("new_key", "new_val");
        col.modifyMetadata(update);

        assertEquals("old", col.getMetadata().get("existing"));
        assertEquals("new_val", col.getMetadata().get("new_key"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCollectionMetadataIsUnmodifiable() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"metadata\":{\"existing\":\"old\"}}")));

        Collection col = client.getCollection("test_col");
        col.getMetadata().put("x", "y");
    }

    // --- add ---

    @Test
    public void testAdd() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add"))
                .withRequestBody(matchingJsonPath("$.ids", containing("id1")))
                .willReturn(aResponse().withStatus(200)));

        collection.add()
                .ids("id1", "id2")
                .embeddings(new float[]{1.0f, 2.0f}, new float[]{3.0f, 4.0f})
                .documents("doc1", "doc2")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add")));
    }

    @Test
    public void testAddWithListArgs() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add"))
                .willReturn(aResponse().withStatus(200)));

        collection.add()
                .ids(Arrays.asList("id1"))
                .embeddings(Collections.singletonList(new float[]{1.0f}))
                .documents(Collections.singletonList("doc1"))
                .metadatas(Collections.singletonList(Collections.<String, Object>singletonMap("k", "v")))
                .uris(Collections.singletonList("uri1"))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRequiresIds() {
        collection.add().documents("doc1").execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddRejectsEmptyIds() {
        collection.add().ids(Collections.<String>emptyList()).execute();
    }

    // --- upsert ---

    @Test
    public void testUpsert() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert"))
                .willReturn(aResponse().withStatus(200)));

        collection.upsert()
                .ids("id1")
                .embeddings(new float[]{1.0f, 2.0f})
                .documents("doc1")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert")));
    }

    @Test
    public void testUpsertWithUris() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert"))
                .willReturn(aResponse().withStatus(200)));

        collection.upsert()
                .ids("id1")
                .uris("uri1")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsertRequiresIds() {
        collection.upsert().documents("doc1").execute();
    }

    // --- update ---

    @Test
    public void testUpdate() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update"))
                .willReturn(aResponse().withStatus(200)));

        collection.update()
                .ids("id1")
                .documents("updated doc")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRequiresIds() {
        collection.update().documents("doc1").execute();
    }

    // --- delete ---

    @Test
    public void testDelete() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.ids", containing("id1")))
                .willReturn(aResponse().withStatus(200)));

        collection.delete()
                .ids("id1")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteWithListIds() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .willReturn(aResponse().withStatus(200)));

        collection.delete()
                .ids(Arrays.asList("id1", "id2"))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteWithNoArgs() {
        try {
            collection.delete().execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at least one criterion"));
        }
    }

    @Test
    public void testDeleteWithWhereAndWhereDocument() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where.topic", equalTo("news")))
                .withRequestBody(matchingJsonPath("$.where_document.$contains", equalTo("ai")))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("topic", "news");
        Map<String, Object> whereDocMap = new LinkedHashMap<String, Object>();
        whereDocMap.put("$contains", "ai");

        collection.delete()
                .where(where(whereMap))
                .whereDocument(whereDocument(whereDocMap))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    // --- query ---

    @Test
    public void testQuery() {
        String responseBody = "{\"ids\":[[\"id1\",\"id2\"]],"
                + "\"documents\":[[\"doc1\",\"doc2\"]],"
                + "\"distances\":[[0.1,0.2]],"
                + "\"metadatas\":[[{\"k\":\"v1\"},{\"k\":\"v2\"}]],"
                + "\"embeddings\":[[[1.0,2.0],[3.0,4.0]]],"
                + "\"uris\":[[\"uri1\",\"uri2\"]]}";

        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f, 2.0f})
                .nResults(5)
                .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS, Include.EMBEDDINGS, Include.URIS)
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
        assertEquals(2, result.getIds().get(0).size());
        assertEquals("id1", result.getIds().get(0).get(0));
        assertEquals("doc1", result.getDocuments().get(0).get(0));
        assertEquals(Float.valueOf(0.1f), result.getDistances().get(0).get(0));
        assertEquals("v1", result.getMetadatas().get(0).get(0).get("k"));
        assertArrayEquals(new float[]{1.0f, 2.0f}, result.getEmbeddings().get(0).get(0), 0.001f);
        assertEquals("uri1", result.getUris().get(0).get(0));
    }

    @Test
    public void testQueryWithListEmbeddings() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        QueryResult result = collection.query()
                .queryEmbeddings(Collections.singletonList(new float[]{1.0f}))
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
    }

    @Test
    public void testQueryWithWhereAndWhereDocument() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .withRequestBody(matchingJsonPath("$.where.topic", equalTo("news")))
                .withRequestBody(matchingJsonPath("$.where_document.$contains", equalTo("ai")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("topic", "news");
        Map<String, Object> whereDocMap = new LinkedHashMap<String, Object>();
        whereDocMap.put("$contains", "ai");

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f})
                .where(where(whereMap))
                .whereDocument(whereDocument(whereDocMap))
                .execute();

        assertEquals(1, result.getIds().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryRequiresEmbeddings() {
        collection.query().nResults(10).execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryRejectsNonPositiveNResults() {
        collection.query().nResults(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testQueryTextsVarargsNotSupported() {
        collection.query().queryTexts("text");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testQueryTextsListNotSupported() {
        collection.query().queryTexts(Collections.singletonList("text"));
    }

    // --- get ---

    @Test
    public void testGet() {
        String responseBody = "{\"ids\":[\"id1\",\"id2\"],"
                + "\"documents\":[\"doc1\",\"doc2\"],"
                + "\"metadatas\":[{\"k\":\"v1\"},{\"k\":\"v2\"}],"
                + "\"embeddings\":[[1.0,2.0],[3.0,4.0]],"
                + "\"uris\":[\"uri1\",\"uri2\"]}";

        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        GetResult result = collection.get()
                .ids("id1", "id2")
                .include(Include.DOCUMENTS, Include.METADATAS, Include.EMBEDDINGS, Include.URIS)
                .execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
        assertEquals("id1", result.getIds().get(0));
        assertEquals("doc1", result.getDocuments().get(0));
        assertEquals("v1", result.getMetadatas().get(0).get("k"));
        assertArrayEquals(new float[]{1.0f, 2.0f}, result.getEmbeddings().get(0), 0.001f);
        assertEquals("uri1", result.getUris().get(0));
    }

    @Test
    public void testGetWithLimitOffset() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .withRequestBody(matchingJsonPath("$.limit", equalTo("10")))
                .withRequestBody(matchingJsonPath("$.offset", equalTo("5")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"]}")));

        GetResult result = collection.get()
                .limit(10)
                .offset(5)
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
    }

    @Test
    public void testGetWithWhereAndWhereDocument() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .withRequestBody(matchingJsonPath("$.where.topic", equalTo("news")))
                .withRequestBody(matchingJsonPath("$.where_document.$contains", equalTo("ai")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"]}")));

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("topic", "news");
        Map<String, Object> whereDocMap = new LinkedHashMap<String, Object>();
        whereDocMap.put("$contains", "ai");

        GetResult result = collection.get()
                .where(where(whereMap))
                .whereDocument(whereDocument(whereDocMap))
                .execute();

        assertEquals(1, result.getIds().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRejectsNegativeLimit() {
        collection.get().limit(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRejectsNegativeOffset() {
        collection.get().offset(-1);
    }

    @Test
    public void testGetAll() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\",\"id2\"]}")));

        GetResult result = collection.get().execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
    }

    @Test
    public void testGetRejectsNullEmbeddingElement() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"],\"embeddings\":[[1.0,null]]}")));

        try {
            collection.get().ids("id1").execute();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("null value at index 1"));
        }
    }

    // --- collection accessors ---

    @Test
    public void testCollectionAccessors() {
        assertEquals("col-id-1", collection.getId());
        assertEquals("test_col", collection.getName());
        assertEquals(Tenant.defaultTenant(), collection.getTenant());
        assertEquals(Database.defaultDatabase(), collection.getDatabase());
    }

    // --- query result null fields ---

    @Test
    public void testQueryResultNullOptionalFields() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f})
                .execute();

        assertNotNull(result.getIds());
        assertNull(result.getDocuments());
        assertNull(result.getMetadatas());
        assertNull(result.getEmbeddings());
        assertNull(result.getDistances());
        assertNull(result.getUris());
    }

    @Test
    public void testQueryResultRequiresIds() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"documents\":[[\"doc1\"]]}")));

        try {
            collection.query().queryEmbeddings(new float[]{1.0f}).execute();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("required ids field"));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testQueryResultIdsOuterListIsUnmodifiable() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f})
                .execute();
        result.getIds().add(Collections.singletonList("id2"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testQueryResultIdsInnerListIsUnmodifiable() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        QueryResult result = collection.query()
                .queryEmbeddings(new float[]{1.0f})
                .execute();
        result.getIds().get(0).add("id2");
    }

    // --- get result null fields ---

    @Test
    public void testGetResultNullOptionalFields() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"]}")));

        GetResult result = collection.get().ids("id1").execute();

        assertNotNull(result.getIds());
        assertNull(result.getDocuments());
        assertNull(result.getMetadatas());
        assertNull(result.getEmbeddings());
        assertNull(result.getUris());
    }

    @Test
    public void testGetResultRequiresIds() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"documents\":[\"doc1\"]}")));

        try {
            collection.get().execute();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("required ids field"));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetResultIdsListIsUnmodifiable() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"]}")));

        GetResult result = collection.get().ids("id1").execute();
        result.getIds().add("id2");
    }

    // --- add with uris ---

    @Test
    public void testAddWithUris() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add"))
                .willReturn(aResponse().withStatus(200)));

        collection.add()
                .ids("id1")
                .uris("https://example.com/doc1")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add")));
    }
}
