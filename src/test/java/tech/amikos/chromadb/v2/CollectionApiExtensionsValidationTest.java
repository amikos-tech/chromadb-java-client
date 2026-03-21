package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * WireMock unit tests for fork, forkCount, and indexingStatus operations on Collection.
 *
 * <p>Tests verify HTTP request/response contracts, field mapping, error propagation,
 * and EF inheritance without requiring a live ChromaDB server.</p>
 */
public class CollectionApiExtensionsValidationTest {

    private static final String COLLECTIONS_PATH =
            "/api/v2/tenants/default_tenant/databases/default_database/collections";

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private Client client;
    private Collection collection;

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

    @Before
    public void setUp() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"source\"}")));

        client = ChromaClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        collection = client.getOrCreateCollection("source");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // --- IndexingStatus value object unit tests ---

    @Test
    public void testIndexingStatusGetNumIndexedOps() {
        IndexingStatus status = IndexingStatus.of(100, 5, 105, 0.952);
        assertEquals(100, status.getNumIndexedOps());
    }

    @Test
    public void testIndexingStatusGetNumUnindexedOps() {
        IndexingStatus status = IndexingStatus.of(100, 5, 105, 0.952);
        assertEquals(5, status.getNumUnindexedOps());
    }

    @Test
    public void testIndexingStatusGetTotalOps() {
        IndexingStatus status = IndexingStatus.of(100, 5, 105, 0.952);
        assertEquals(105, status.getTotalOps());
    }

    @Test
    public void testIndexingStatusGetOpIndexingProgress() {
        IndexingStatus status = IndexingStatus.of(100, 5, 105, 0.952);
        assertEquals(0.952, status.getOpIndexingProgress(), 0.001);
    }

    @Test
    public void testIndexingStatusEqualsAndHashCode() {
        IndexingStatus a = IndexingStatus.of(100, 5, 105, 0.952);
        IndexingStatus b = IndexingStatus.of(100, 5, 105, 0.952);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testIndexingStatusToStringContainsNumIndexedOps() {
        IndexingStatus status = IndexingStatus.of(100, 5, 105, 0.952);
        assertTrue("toString should contain numIndexedOps=100",
                status.toString().contains("numIndexedOps=100"));
    }

    // --- fork ---

    @Test
    public void testForkSendsPostAndReturnsCollection() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
                .withRequestBody(matchingJsonPath("$.new_name", equalTo("forked")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"fork-id-1\",\"name\":\"forked\"}")));

        Collection forked = collection.fork("forked");

        assertEquals("forked", forked.getName());
        assertEquals("fork-id-1", forked.getId());

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
                .withRequestBody(matchingJsonPath("$.new_name", equalTo("forked"))));
    }

    @Test
    public void testForkedCollectionInheritsEmbeddingFunction() throws Exception {
        // Create a collection with an explicit EF
        EmbeddingFunction ef = fixedEmbeddingFunction(new float[]{0.1f, 0.2f, 0.3f});
        Collection sourceWithEf = client.getOrCreateCollection("source",
                CreateCollectionOptions.builder().embeddingFunction(ef).build());

        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"fork-id-2\",\"name\":\"forked-ef\"}")));

        Collection forked = sourceWithEf.fork("forked-ef");

        // Verify the forked collection carries the EF (via reflection on the package-private field)
        Field efField = forked.getClass().getDeclaredField("explicitEmbeddingFunction");
        efField.setAccessible(true);
        Object storedEf = efField.get(forked);
        assertNotNull("Forked collection must inherit the source embedding function", storedEf);
        assertSame(ef, storedEf);
    }

    @Test(expected = NullPointerException.class)
    public void testForkRejectsNullName() {
        collection.fork(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForkRejectsEmptyName() {
        collection.fork("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForkRejectsBlankName() {
        collection.fork("  ");
    }

    // --- forkCount ---

    @Test
    public void testForkCountReturnsCount() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork_count"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":3}")));

        assertEquals(3, collection.forkCount());
    }

    @Test
    public void testForkCountReturnsZero() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork_count"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":0}")));

        assertEquals(0, collection.forkCount());
    }

    // --- indexingStatus ---

    @Test
    public void testIndexingStatusMapsAllFields() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/indexing_status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"num_indexed_ops\":100,\"num_unindexed_ops\":5,"
                                + "\"total_ops\":105,\"op_indexing_progress\":0.952}")));

        IndexingStatus status = collection.indexingStatus();

        assertEquals(100, status.getNumIndexedOps());
        assertEquals(5, status.getNumUnindexedOps());
        assertEquals(105, status.getTotalOps());
        assertEquals(0.952, status.getOpIndexingProgress(), 0.001);
    }

    // --- Error propagation ---

    @Test(expected = ChromaNotFoundException.class)
    public void testForkServerError404ThrowsNotFoundException() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/fork"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        collection.fork("x");
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testIndexingStatusServerError404ThrowsNotFoundException() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/indexing_status"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        collection.indexingStatus();
    }
}
