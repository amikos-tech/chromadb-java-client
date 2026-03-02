package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
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

    private static EmbeddingFunction fixedEmbeddingFunction(final float[] embedding) {
        return new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) throws EFException {
                return new Embedding(embedding);
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) throws EFException {
                List<Embedding> result = new ArrayList<Embedding>(documents.size());
                for (int i = 0; i < documents.size(); i++) {
                    result.add(new Embedding(embedding));
                }
                return result;
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) throws EFException {
                return embedDocuments(Arrays.asList(documents));
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) throws EFException {
                return embedDocuments(queries);
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) throws EFException {
                return embedQueries(Arrays.asList(queries));
            }
        };
    }

    private static String schemaWithEmbeddingProviderJson(String providerName) {
        return "{"
                + "\"keys\":{"
                + "\"" + Schema.EMBEDDING_KEY + "\":{"
                + "\"float_list\":{"
                + "\"vector_index\":{"
                + "\"enabled\":true,"
                + "\"config\":{"
                + "\"embedding_function\":{"
                + "\"type\":\"known\","
                + "\"name\":\"" + providerName + "\","
                + "\"config\":{}"
                + "}"
                + "}"
                + "}"
                + "}"
                + "}"
                + "}"
                + "}";
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

    @Test(expected = NullPointerException.class)
    public void testModifyMetadataRejectsNull() {
        collection.modifyMetadata(null);
    }

    // --- modifyConfiguration ---

    @Test
    public void testModifyConfigurationHnsw() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.ef_search", equalTo("200")))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.num_threads", equalTo("4")))
                .willReturn(aResponse().withStatus(200)));

        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .hnswNumThreads(4)
                .build();
        collection.modifyConfiguration(cfg);

        assertNotNull(collection.getConfiguration());
        assertEquals(Integer.valueOf(200), collection.getConfiguration().getHnswSearchEf());
        assertEquals(Integer.valueOf(4), collection.getConfiguration().getHnswNumThreads());
    }

    @Test
    public void testModifyConfigurationHnswNumThreadsAndResizeFromNullConfiguration() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.num_threads", equalTo("6")))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.resize_factor", equalTo("1.25")))
                .willReturn(aResponse().withStatus(200)));

        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .hnswNumThreads(6)
                .hnswResizeFactor(1.25)
                .build();
        collection.modifyConfiguration(cfg);

        assertNotNull(collection.getConfiguration());
        assertEquals(Integer.valueOf(6), collection.getConfiguration().getHnswNumThreads());
        assertEquals(Double.valueOf(1.25), collection.getConfiguration().getHnswResizeFactor());
    }

    @Test
    public void testModifyConfigurationSpann() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_configuration.spann.search_nprobe", equalTo("32")))
                .withRequestBody(matchingJsonPath("$.new_configuration.spann.ef_search", equalTo("64")))
                .willReturn(aResponse().withStatus(200)));

        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .spannSearchNprobe(32)
                .spannEfSearch(64)
                .build();
        collection.modifyConfiguration(cfg);

        assertNotNull(collection.getConfiguration());
        assertEquals(Integer.valueOf(32), collection.getConfiguration().getSpannSearchNprobe());
        assertEquals(Integer.valueOf(64), collection.getConfiguration().getSpannEfSearch());
    }

    @Test
    public void testModifyConfigurationPreservesExistingConfigurationFields() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"configuration_json\":{\"hnsw:space\":\"cosine\",\"hnsw:M\":16,\"hnsw:construction_ef\":100,\"hnsw:search_ef\":50,\"hnsw:num_threads\":2}}")));
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.ef_search", equalTo("200")))
                .willReturn(aResponse().withStatus(200)));

        Collection col = client.getCollection("test_col");
        col.modifyConfiguration(UpdateCollectionConfiguration.builder().hnswSearchEf(200).build());

        assertNotNull(col.getConfiguration());
        assertEquals(DistanceFunction.COSINE, col.getConfiguration().getSpace());
        assertEquals(Integer.valueOf(16), col.getConfiguration().getHnswM());
        assertEquals(Integer.valueOf(100), col.getConfiguration().getHnswConstructionEf());
        assertEquals(Integer.valueOf(2), col.getConfiguration().getHnswNumThreads());
        assertEquals(Integer.valueOf(200), col.getConfiguration().getHnswSearchEf());
    }

    @Test
    public void testModifyConfigurationPreservesSchemaAndEmbeddingFunctionSpec() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"configuration_json\":{"
                                + "\"hnsw:space\":\"cosine\","
                                + "\"hnsw:search_ef\":50,"
                                + "\"embedding_function\":{\"type\":\"known\",\"name\":\"openai\",\"config\":{\"api_key_env_var\":\"OPENAI_API_KEY\"}},"
                                + "\"schema\":{"
                                + "\"keys\":{"
                                + "\"" + Schema.EMBEDDING_KEY + "\":{"
                                + "\"float_list\":{"
                                + "\"vector_index\":{"
                                + "\"enabled\":true,"
                                + "\"config\":{"
                                + "\"space\":\"ip\","
                                + "\"embedding_function\":{\"type\":\"known\",\"name\":\"cohere\",\"config\":{\"api_key_env_var\":\"COHERE_API_KEY\"}}"
                                + "}"
                                + "}"
                                + "}"
                                + "}"
                                + "}"
                                + "}"
                                + "}}")));
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .withRequestBody(matchingJsonPath("$.new_configuration.hnsw.ef_search", equalTo("200")))
                .willReturn(aResponse().withStatus(200)));

        Collection col = client.getCollection("test_col");
        col.modifyConfiguration(UpdateCollectionConfiguration.builder().hnswSearchEf(200).build());

        assertNotNull(col.getConfiguration());
        assertEquals(Integer.valueOf(200), col.getConfiguration().getHnswSearchEf());
        assertNotNull(col.getConfiguration().getEmbeddingFunction());
        assertEquals("openai", col.getConfiguration().getEmbeddingFunction().getName());
        assertNotNull(col.getConfiguration().getSchema());
        assertNotNull(col.getConfiguration().getSchema().getDefaultEmbeddingFunctionSpec());
        assertEquals("cohere", col.getConfiguration().getSchema().getDefaultEmbeddingFunctionSpec().getName());
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testModifyConfigurationPropagatesNotFound() {
        stubFor(put(urlEqualTo(COLLECTIONS_PATH + "/col-id-1"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        collection.modifyConfiguration(
                UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(200)
                        .build()
        );
    }

    @Test(expected = NullPointerException.class)
    public void testModifyConfigurationRejectsNull() {
        collection.modifyConfiguration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModifyConfigurationRejectsEmpty() {
        collection.modifyConfiguration(UpdateCollectionConfiguration.builder().build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testModifyConfigurationRejectsMixedGroups() {
        collection.modifyConfiguration(
                UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(200)
                        .spannEfSearch(64)
                        .build()
        );
    }

    @Test
    public void testModifyConfigurationRejectsSwitchFromHnswToSpannClientSide() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"configuration_json\":{\"hnsw:search_ef\":50}}")));

        Collection col = client.getCollection("test_col");

        try {
            col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                    .spannSearchNprobe(32)
                    .build());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot switch collection index parameters between HNSW and SPANN"));
        }

        verify(0, putRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1")));
    }

    @Test
    public void testModifyConfigurationRejectsSwitchFromSpannToHnswClientSide() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"configuration_json\":{\"spann:search_nprobe\":32}}")));

        Collection col = client.getCollection("test_col");

        try {
            col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                    .hnswSearchEf(120)
                    .build());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot switch collection index parameters between HNSW and SPANN"));
        }

        verify(0, putRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1")));
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

    @Test(expected = IllegalArgumentException.class)
    public void testAddRejectsMismatchedDocumentsSize() {
        collection.add()
                .ids("id1", "id2")
                .documents("doc1")
                .execute();
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

    @Test
    public void testUpsertWithListArgs() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert"))
                .willReturn(aResponse().withStatus(200)));

        collection.upsert()
                .ids(Collections.singletonList("id1"))
                .embeddings(Collections.singletonList(new float[]{1.0f}))
                .documents(Collections.singletonList("doc1"))
                .metadatas(Collections.singletonList(Collections.<String, Object>singletonMap("k", "v")))
                .uris(Collections.singletonList("uri1"))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsertRequiresIds() {
        collection.upsert().documents("doc1").execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsertRejectsMismatchedEmbeddingsSize() {
        collection.upsert()
                .ids("id1", "id2")
                .embeddings(Collections.singletonList(new float[]{1.0f}))
                .execute();
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

    @Test
    public void testUpdateWithListArgs() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update"))
                .willReturn(aResponse().withStatus(200)));

        collection.update()
                .ids(Collections.singletonList("id1"))
                .embeddings(Collections.singletonList(new float[]{1.0f}))
                .documents(Collections.singletonList("doc1"))
                .metadatas(Collections.singletonList(Collections.<String, Object>singletonMap("k", "v")))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRequiresIds() {
        collection.update().documents("doc1").execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRejectsMismatchedMetadatasSize() {
        collection.update()
                .ids("id1", "id2")
                .metadatas(Collections.singletonList(Collections.<String, Object>singletonMap("k", "v")))
                .execute();
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
    public void testDeleteWithWhereOnly() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where.topic", equalTo("news")))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("topic", "news");

        collection.delete()
                .where(where(whereMap))
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
    public void testDeleteWithInlineDocumentWhereFilter() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where['#document']['$contains']", equalTo("ai")))
                .willReturn(aResponse().withStatus(200)));

        collection.delete()
                .where(Where.documentContains("ai"))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteAllowsInlineIdWhereFilterShape() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where['#id']['$in'][0]", equalTo("id1")))
                .withRequestBody(matchingJsonPath("$.where['#id']['$in'][1]", equalTo("id2")))
                .willReturn(aResponse().withStatus(200)));

        collection.delete()
                .where(Where.idIn("id1", "id2"))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteAllowsNestedInlineIdWhereFilterShape() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where['$and'][0].topic", equalTo("news")))
                .withRequestBody(matchingJsonPath("$.where['$and'][1]['#id']['$nin'][0]", equalTo("id1")))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> whereMap = new LinkedHashMap<String, Object>();
        whereMap.put("topic", "news");

        collection.delete()
                .where(Where.and(where(whereMap), Where.idNotIn("id1")))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteAllowsDeeplyNestedInlineIdWhereFilterShape() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.where['$and'][0].topic", equalTo("news")))
                .withRequestBody(matchingJsonPath(
                        "$.where['$and'][1]['$or'][1]['$and'][1]['#id']['$in'][0]",
                        equalTo("id1")
                ))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> topic = new LinkedHashMap<String, Object>();
        topic.put("topic", "news");
        Map<String, Object> region = new LinkedHashMap<String, Object>();
        region.put("region", "eu");
        Map<String, Object> lang = new LinkedHashMap<String, Object>();
        lang.put("lang", "en");

        collection.delete()
                .where(Where.and(
                        where(topic),
                        Where.or(
                                where(region),
                                Where.and(where(lang), Where.idIn("id1"))
                        )
                ))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteAcceptsNestedWhereWithoutInlineIdFilters() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .willReturn(aResponse().withStatus(200)));

        Map<String, Object> topic = new LinkedHashMap<String, Object>();
        topic.put("topic", "news");
        Map<String, Object> region = new LinkedHashMap<String, Object>();
        region.put("region", "eu");
        Map<String, Object> lang = new LinkedHashMap<String, Object>();
        lang.put("lang", "en");

        collection.delete()
                .where(Where.and(
                        where(topic),
                        Where.or(where(region), where(lang))
                ))
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete")));
    }

    @Test
    public void testDeleteRejectsWhereToMapReturningNull() {
        try {
            collection.delete()
                    .where(new Where() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("toMap"));
        }
    }

    @Test
    public void testDeleteRejectsWhereDocumentToMapReturningNull() {
        try {
            collection.delete()
                    .whereDocument(new WhereDocument() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("whereDocument.toMap"));
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

    @Test
    public void testQueryRejectsWhereToMapReturningNull() {
        try {
            collection.query()
                    .queryEmbeddings(new float[]{1.0f})
                    .where(new Where() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("where.toMap"));
        }
    }

    @Test
    public void testQueryRejectsWhereDocumentToMapReturningNull() {
        try {
            collection.query()
                    .queryEmbeddings(new float[]{1.0f})
                    .whereDocument(new WhereDocument() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("whereDocument.toMap"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryRequiresEmbeddings() {
        collection.query().nResults(10).execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryRejectsNonPositiveNResults() {
        collection.query().nResults(0);
    }

    @Test
    public void testQueryTextsWithoutEmbeddingFunctionFailsOnExecute() {
        try {
            collection.query().queryTexts("text").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("queryTexts requires an embedding function"));
        }
    }

    @Test
    public void testQueryRejectsMixedQueryTextsAndEmbeddings() {
        try {
            collection.query()
                    .queryTexts("text")
                    .queryEmbeddings(new float[]{1.0f});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot set both queryTexts and queryEmbeddings"));
        }
    }

    @Test
    public void testQueryRejectsMixedQueryEmbeddingsAndTexts() {
        try {
            collection.query()
                    .queryEmbeddings(new float[]{1.0f})
                    .queryTexts("text");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot set both queryTexts and queryEmbeddings"));
        }
    }

    @Test
    public void testQueryTextsSuccessBuildsQueryEmbeddings() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-id\",\"name\":\"query_texts_col\"}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/query-texts-id/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.5")))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][1]", equalTo("1.5")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(fixedEmbeddingFunction(new float[]{0.5f, 1.5f}))
                        .build()
        );

        QueryResult result = textQueryCollection.query()
                .queryTexts("hello")
                .execute();

        assertNotNull(result);
        assertEquals("id1", result.getIds().get(0).get(0));
        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/query-texts-id/query")));
    }

    @Test
    public void testQueryTextsMultipleTextsBuildsMultipleQueryEmbeddings() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_multi_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-multi-id\",\"name\":\"query_texts_multi_col\"}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/query-texts-multi-id/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.1")))
                .withRequestBody(matchingJsonPath("$.query_embeddings[1][0]", equalTo("0.1")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"],[\"id2\"]]}")));

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_multi_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(fixedEmbeddingFunction(new float[]{0.1f, 0.2f}))
                        .build()
        );

        QueryResult result = textQueryCollection.query()
                .queryTexts("hello", "world")
                .execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/query-texts-multi-id/query")));
    }

    @Test
    public void testQueryTextsUsesEmbedQueriesWhenAvailable() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_embed_queries_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-embed-queries-id\",\"name\":\"query_texts_embed_queries_col\"}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/query-texts-embed-queries-id/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.9")))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][1]", equalTo("0.1")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        EmbeddingFunction embeddingFunction = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                throw new IllegalStateException("embedQuery must not be used");
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) {
                return Collections.singletonList(new Embedding(new float[]{0.9f, 0.1f}));
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) {
                return embedQueries(Arrays.asList(queries));
            }
        };

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_embed_queries_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(embeddingFunction)
                        .build()
        );

        QueryResult result = textQueryCollection.query()
                .queryTexts("hello")
                .execute();

        assertNotNull(result);
        assertEquals("id1", result.getIds().get(0).get(0));
    }

    @Test
    public void testQueryTextsUsesExplicitEmbedQueriesInsteadOfEmbedDocuments() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_no_embed_documents_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-no-embed-docs-id\",\"name\":\"query_texts_no_embed_documents_col\"}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/query-texts-no-embed-docs-id/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.33")))
                .withRequestBody(matchingJsonPath("$.query_embeddings[1][0]", equalTo("0.66")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"],[\"id2\"]]}")));

        EmbeddingFunction embeddingFunction = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                throw new IllegalStateException("embedQuery must not be used");
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) {
                List<Embedding> out = new ArrayList<Embedding>(queries.size());
                out.add(new Embedding(new float[]{0.33f}));
                out.add(new Embedding(new float[]{0.66f}));
                return out;
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) {
                return embedQueries(Arrays.asList(queries));
            }
        };

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_no_embed_documents_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(embeddingFunction)
                        .build()
        );

        QueryResult result = textQueryCollection.query()
                .queryTexts("first", "second")
                .execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
    }

    @Test
    public void testQueryTextsRejectsEmbeddingCountMismatch() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_mismatch_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-mismatch-id\",\"name\":\"query_texts_mismatch_col\"}")));

        EmbeddingFunction embeddingFunction = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                return new Embedding(new float[]{0.1f});
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                throw new IllegalStateException("embedDocuments must not be used");
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) {
                return Collections.singletonList(new Embedding(new float[]{0.5f}));
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) {
                return embedQueries(Arrays.asList(queries));
            }
        };

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_mismatch_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(embeddingFunction)
                        .build()
        );

        try {
            textQueryCollection.query().queryTexts("one", "two").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("returned 1 embeddings for 2 query texts"));
        }
    }

    @Test
    public void testQueryTextsRejectsEmptyInput() {
        try {
            collection.query().queryTexts(Collections.<String>emptyList());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("queryTexts must not be empty"));
        }
    }

    @Test
    public void testQueryTextsRejectsNullList() {
        try {
            collection.query().queryTexts((List<String>) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("texts"));
        }
    }

    @Test
    public void testQueryTextsRejectsNullVarargsArray() {
        try {
            collection.query().queryTexts((String[]) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("texts"));
        }
    }

    @Test
    public void testQueryTextsRejectsNullElement() {
        try {
            collection.query().queryTexts(Arrays.asList("ok", null));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("queryTexts[1] must not be null"));
        }
    }

    @Test
    public void testQueryTextsUnknownProviderFailsFast() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/unknown_provider_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-unknown\",\"name\":\"unknown_provider_col\","
                                + "\"configuration_json\":{\"embedding_function\":{\"type\":\"known\",\"name\":\"consistent_hash\",\"config\":{}}}}")));

        Collection col = client.getCollection("unknown_provider_col");
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("consistent_hash"));
            assertTrue(e.getMessage().contains("queryEmbeddings"));
        }
    }

    @Test
    public void testEmbeddingFunctionSpecPrecedencePrefersConfigurationEmbeddingFunction() {
        String topLevelSchema = schemaWithEmbeddingProviderJson("unknown_top");
        String configSchema = schemaWithEmbeddingProviderJson("unknown_cfg");
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/ef_precedence_cfg_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-ef-precedence-cfg\",\"name\":\"ef_precedence_cfg_col\","
                                + "\"schema\":" + topLevelSchema + ","
                                + "\"configuration_json\":{"
                                + "\"embedding_function\":{\"type\":\"known\",\"name\":\"consistent_hash\",\"config\":{}},"
                                + "\"schema\":" + configSchema
                                + "}}")));

        Collection col = client.getCollection("ef_precedence_cfg_col");
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("consistent_hash"));
        }
    }

    @Test
    public void testEmbeddingFunctionSpecPrecedencePrefersTopLevelSchemaOverConfigurationSchema() {
        String topLevelSchema = schemaWithEmbeddingProviderJson("unknown_top");
        String configSchema = schemaWithEmbeddingProviderJson("unknown_cfg");
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/ef_precedence_top_schema_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-ef-precedence-top\",\"name\":\"ef_precedence_top_schema_col\","
                                + "\"schema\":" + topLevelSchema + ","
                                + "\"configuration_json\":{\"schema\":" + configSchema + "}}")));

        Collection col = client.getCollection("ef_precedence_top_schema_col");
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("unknown_top"));
        }
    }

    @Test
    public void testEmbeddingFunctionSpecPrecedenceFallsBackToConfigurationSchema() {
        String configSchema = schemaWithEmbeddingProviderJson("consistent_hash");
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/ef_precedence_cfg_schema_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-ef-precedence-cfg-schema\",\"name\":\"ef_precedence_cfg_schema_col\","
                                + "\"configuration_json\":{\"schema\":" + configSchema + "}}")));

        Collection col = client.getCollection("ef_precedence_cfg_schema_col");
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("consistent_hash"));
        }
    }

    @Test
    public void testQueryTextsProviderConfigFailureIsWrapped() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/openai_provider_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-openai\",\"name\":\"openai_provider_col\","
                                + "\"configuration_json\":{\"embedding_function\":{"
                                + "\"type\":\"known\","
                                + "\"name\":\"openai\","
                                + "\"config\":{\"api_key_env_var\":\"CHROMA_NON_EXISTENT_OPENAI_ENV_123\"}"
                                + "}}}")));

        Collection col = client.getCollection("openai_provider_col");
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to initialize embedding function provider 'openai'"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testQueryTextsEmbeddingFailureIsWrapped() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_fail_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-fail-id\",\"name\":\"query_texts_fail_col\"}")));

        EmbeddingFunction failingEmbeddingFunction = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) throws EFException {
                throw new EFException("query embedding failed");
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) throws EFException {
                throw new EFException("document embedding failed");
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) throws EFException {
                throw new EFException("document embedding failed");
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) throws EFException {
                throw new EFException("query embeddings failed");
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) throws EFException {
                throw new EFException("query embeddings failed");
            }
        };

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_fail_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(failingEmbeddingFunction)
                        .build()
        );

        try {
            textQueryCollection.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to embed queryTexts"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testQueryTextsRuntimeEmbeddingFailureIsWrapped() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .withRequestBody(matchingJsonPath("$.name", equalTo("query_texts_runtime_fail_col")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"query-texts-runtime-fail-id\",\"name\":\"query_texts_runtime_fail_col\"}")));

        EmbeddingFunction failingEmbeddingFunction = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                throw new IllegalStateException("query runtime failure");
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                throw new IllegalStateException("document runtime failure");
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                throw new IllegalStateException("document runtime failure");
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) {
                throw new IllegalStateException("query runtime failure");
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) {
                throw new IllegalStateException("query runtime failure");
            }
        };

        Collection textQueryCollection = client.getOrCreateCollection(
                "query_texts_runtime_fail_col",
                CreateCollectionOptions.builder()
                        .embeddingFunction(failingEmbeddingFunction)
                        .build()
        );

        try {
            textQueryCollection.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to embed queryTexts"));
            assertNotNull(e.getCause());
        }
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

    @Test
    public void testGetRejectsWhereToMapReturningNull() {
        try {
            collection.get()
                    .where(new Where() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("where.toMap"));
        }
    }

    @Test
    public void testGetRejectsWhereDocumentToMapReturningNull() {
        try {
            collection.get()
                    .whereDocument(new WhereDocument() {
                        @Override
                        public Map<String, Object> toMap() {
                            return null;
                        }
                    })
                    .execute();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("whereDocument.toMap"));
        }
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

    // --- factory input guards ---

    @Test(expected = NullPointerException.class)
    public void testFromRejectsNullApiClient() {
        ChromaHttpCollection.from(validCollectionDto(), null, Tenant.defaultTenant(), Database.defaultDatabase(), null);
    }

    @Test
    public void testFromRejectsNullTenant() {
        ChromaApiClient api = new ChromaApiClient(
                "http://localhost:" + wireMock.port(), null, null, null, null, null);
        try {
            ChromaHttpCollection.from(validCollectionDto(), api, null, Database.defaultDatabase(), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("tenant", e.getMessage());
        } finally {
            api.close();
        }
    }

    @Test
    public void testFromRejectsNullDatabase() {
        ChromaApiClient api = new ChromaApiClient(
                "http://localhost:" + wireMock.port(), null, null, null, null, null);
        try {
            ChromaHttpCollection.from(validCollectionDto(), api, Tenant.defaultTenant(), null, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("database", e.getMessage());
        } finally {
            api.close();
        }
    }

    private static ChromaDtos.CollectionResponse validCollectionDto() {
        ChromaDtos.CollectionResponse dto = new ChromaDtos.CollectionResponse();
        dto.id = "col-id";
        dto.name = "col-name";
        return dto;
    }
}
