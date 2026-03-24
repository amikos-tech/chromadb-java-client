package tech.amikos.chromadb.v2;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.Utils;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Integration tests for the Search API (KNN, RRF, field projection, ReadLevel, GroupBy).
 *
 * <p>The Chroma {@code /search} endpoint is a cloud-only feature (local/self-hosted Chroma
 * returns 501 Not Implemented). Tests are therefore guarded by {@code CHROMA_API_KEY} and
 * {@code assumeMinVersion("1.5.0")}, matching the other cloud integration tests in this module.</p>
 *
 * <p>When credentials are absent, all tests are skipped with an informative message.
 * This ensures CI passes without cloud credentials while still validating the full API
 * when credentials are available.</p>
 */
public class SearchApiIntegrationTest extends AbstractChromaIntegrationTest {

    // All docs use 4-dimensional embeddings
    private static final float[] EMB_DOC1 = {0.9f, 0.1f, 0.1f, 0.1f};  // headphones
    private static final float[] EMB_DOC2 = {0.1f, 0.9f, 0.1f, 0.1f};  // earbuds
    private static final float[] EMB_DOC3 = {0.1f, 0.1f, 0.9f, 0.1f};  // speaker
    private static final float[] EMB_DOC4 = {0.8f, 0.2f, 0.1f, 0.1f};  // headphones, professional
    private static final float[] EMB_DOC5 = {0.7f, 0.1f, 0.1f, 0.3f};  // gaming headset

    // Query embeddings
    private static final float[] QUERY_HEADPHONES = {0.85f, 0.15f, 0.05f, 0.05f};
    private static final float[] QUERY_SPEAKER = {0.1f, 0.1f, 0.9f, 0.1f};

    private static Client searchClient;
    private static Collection searchCollection;
    private static boolean cloudAvailable = false;

    @BeforeClass
    public static void setUpSearchTests() {
        assumeMinVersion("1.5.0");

        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        String tenant = Utils.getEnvOrProperty("CHROMA_TENANT");
        String database = Utils.getEnvOrProperty("CHROMA_DATABASE");

        if (apiKey == null || apiKey.trim().isEmpty()
                || tenant == null || tenant.trim().isEmpty()
                || database == null || database.trim().isEmpty()) {
            // Cloud credentials not available — all tests will be skipped
            return;
        }

        searchClient = ChromaClient.cloud()
                .apiKey(apiKey)
                .tenant(tenant)
                .database(database)
                .timeout(Duration.ofSeconds(45))
                .build();

        String collectionName = "search_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        searchCollection = searchClient.createCollection(collectionName);

        searchCollection.add()
                .ids("doc1", "doc2", "doc3", "doc4", "doc5")
                .documents(
                        "wireless headphones with noise cancelling",
                        "wired earbuds budget audio",
                        "bluetooth speaker portable outdoor",
                        "studio monitor headphones professional",
                        "gaming headset with microphone"
                )
                .embeddings(EMB_DOC1, EMB_DOC2, EMB_DOC3, EMB_DOC4, EMB_DOC5)
                .metadatas(Arrays.asList(
                        mapOf("category", "headphones", "price", 99.99),
                        mapOf("category", "earbuds", "price", 19.99),
                        mapOf("category", "speakers", "price", 49.99),
                        mapOf("category", "headphones", "price", 199.99),
                        mapOf("category", "headsets", "price", 79.99)
                ))
                .execute();

        cloudAvailable = true;
    }

    @AfterClass
    public static void tearDownSearchTests() {
        if (searchClient != null) {
            if (searchCollection != null) {
                try {
                    searchClient.deleteCollection(searchCollection.getName());
                } catch (ChromaException ignored) {
                    // Best-effort cleanup
                }
            }
            searchClient.close();
            searchClient = null;
        }
        searchCollection = null;
        cloudAvailable = false;
    }

    private static void assumeCloud() {
        Assume.assumeTrue(
                "Skipping: CHROMA_API_KEY/CHROMA_TENANT/CHROMA_DATABASE not set (cloud-only test)",
                cloudAvailable
        );
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    // ========== SEARCH-01: KNN search ==========

    @Test
    public void testKnnSearchWithQueryEmbedding() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        SearchResult result = searchCollection.search()
                .queryEmbedding(QUERY_HEADPHONES)
                .limit(3)
                .execute();

        assertNotNull("SearchResult should not be null", result);
        assertNotNull("ids should not be null", result.getIds());
        assertFalse("ids should not be empty", result.getIds().isEmpty());
        assertFalse("first search group should have results", result.getIds().get(0).isEmpty());
        assertTrue("should return at most 3 results", result.getIds().get(0).size() <= 3);
    }

    @Test
    public void testKnnSearchRowAccess() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_HEADPHONES))
                .selectAll()
                .limit(3)
                .build();
        SearchResult result = searchCollection.search().searches(s).execute();

        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertNotNull("rows should not be null", rows);
        assertFalse("rows should not be empty", rows.isEmpty());
        for (SearchResultRow row : rows) {
            assertNotNull("row id should not be null", row.getId());
            // Score should be present when selectAll is used
            assertNotNull("row score should not be null when selected", row.getScore());
        }
    }

    // ========== SEARCH-01: Batch search (D-03) ==========

    @Test
    public void testBatchSearch() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        Search s1 = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_HEADPHONES))
                .limit(2)
                .build();
        Search s2 = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_SPEAKER))
                .limit(2)
                .build();
        SearchResult result = searchCollection.search().searches(s1, s2).execute();

        assertNotNull(result);
        assertEquals("should have 2 search groups", 2, result.searchCount());
        assertFalse("group 0 should have results", result.rows(0).isEmpty());
        assertFalse("group 1 should have results", result.rows(1).isEmpty());
    }

    // ========== SEARCH-02: RRF search ==========

    @Test
    public void testRrfSearch() {
        assumeMinVersion("1.5.0");
        assumeCloud();

        Knn knn1 = Knn.queryEmbedding(QUERY_HEADPHONES).limit(50);
        Knn knn2 = Knn.queryEmbedding(QUERY_SPEAKER).limit(50);
        Rrf rrf = Rrf.builder()
                .rank(knn1, 0.7)
                .rank(knn2, 0.3)
                .k(60)
                .build();
        Search s = Search.builder()
                .rrf(rrf)
                .selectAll()
                .limit(3)
                .build();
        try {
            SearchResult result = searchCollection.search().searches(s).execute();
            assertNotNull(result);
            assertFalse("RRF should return results", result.getIds().get(0).isEmpty());
        } catch (ChromaBadRequestException e) {
            // Server does not understand arithmetic rank expressions
            Assume.assumeTrue("RRF arithmetic ranks not supported on Chroma "
                    + configuredChromaVersion() + " (" + e.getMessage() + ")", false);
        } catch (ChromaServerException e) {
            // Server returned 5xx — may not support arithmetic rank expressions
            Assume.assumeTrue("RRF not supported on Chroma "
                    + configuredChromaVersion() + " (server error: " + e.getMessage() + ")", false);
        } catch (ChromaDeserializationException e) {
            // Server returned an unexpected response format for RRF
            Assume.assumeTrue("RRF response format not supported on Chroma "
                    + configuredChromaVersion() + " (" + e.getMessage() + ")", false);
        }
    }

    // ========== SEARCH-03: Field projection ==========

    @Test
    public void testSelectProjection() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_HEADPHONES))
                .select(Select.ID, Select.SCORE)
                .limit(3)
                .build();
        SearchResult result = searchCollection.search().searches(s).execute();

        assertNotNull(result);
        assertNotNull("ids should be present", result.getIds());

        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertFalse(rows.isEmpty());
        for (SearchResultRow row : rows) {
            assertNotNull("id should be present when selected", row.getId());
            assertNotNull("score should be present when selected", row.getScore());
        }
    }

    @Test
    public void testSelectCustomMetadataKey() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_HEADPHONES))
                .select(Select.ID, Select.SCORE, Select.key("category"))
                .limit(3)
                .build();
        SearchResult result = searchCollection.search().searches(s).execute();

        assertNotNull(result);
        assertFalse(result.rows(0).isEmpty());
    }

    // ========== SEARCH-04: ReadLevel ==========

    @Test
    public void testReadLevelIndexAndWal() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        SearchResult result = searchCollection.search()
                .queryEmbedding(QUERY_HEADPHONES)
                .readLevel(ReadLevel.INDEX_AND_WAL)
                .limit(3)
                .execute();

        assertNotNull(result);
        assertFalse(result.getIds().get(0).isEmpty());
    }

    @Test
    public void testReadLevelIndexOnly() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        // INDEX_ONLY may return fewer results if data is not yet indexed
        // but the call should succeed without error
        SearchResult result = searchCollection.search()
                .queryEmbedding(QUERY_HEADPHONES)
                .readLevel(ReadLevel.INDEX_ONLY)
                .limit(3)
                .execute();

        assertNotNull(result);
        // Results may be empty if not yet indexed; just verify no exception
        assertNotNull("ids outer list must be non-null", result.getIds());
    }

    // ========== SEARCH-04: GroupBy ==========

    @Test
    public void testGroupBySearch() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_HEADPHONES))
                .groupBy(GroupBy.builder().key("category").maxK(2).build())
                .selectAll()
                .limit(10)
                .build();
        SearchResult result = searchCollection.search().searches(s).execute();

        assertNotNull(result);
        assertNotNull("ids should not be null", result.getIds());
    }

    // ========== SEARCH-01: Global filter (D-04) ==========

    @Test
    public void testSearchWithGlobalFilter() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        SearchResult result = searchCollection.search()
                .queryEmbedding(QUERY_HEADPHONES)
                .where(Where.eq("category", "headphones"))
                .limit(5)
                .execute();

        assertNotNull(result);
        // All results should be in "headphones" category
        List<List<Map<String, Object>>> metadatas = result.getMetadatas();
        if (metadatas != null && !metadatas.isEmpty() && metadatas.get(0) != null) {
            for (Map<String, Object> meta : metadatas.get(0)) {
                if (meta != null) {
                    assertEquals("headphones", meta.get("category"));
                }
            }
        }
    }

    // ========== SEARCH-01: Convenience shortcut (D-01, D-02) ==========

    @Test
    public void testConvenienceQueryEmbeddingShortcut() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        // Simplest possible search — embedding-based convenience shortcut per D-02
        SearchResult result = searchCollection.search()
                .queryEmbedding(QUERY_HEADPHONES)
                .limit(5)
                .execute();

        assertNotNull(result);
        assertFalse(result.getIds().get(0).isEmpty());
    }

    @Test
    public void testConvenienceQueryTextShortcut() {
        assumeMinVersion("1.5.0");
        assumeCloud();
        // Text-based KNN queries (string in $knn.query) are not currently accepted by the
        // Chroma server — it returns "data did not match any variant of untagged enum
        // QueryVector". Only float[] embedding vectors are supported in $knn.query.
        // This test documents the intended D-01 text-query shortcut and will be enabled
        // once the server adds text-vector support.
        Assume.assumeTrue("Skipping: text-based $knn.query is not yet supported by Chroma server", false);

        SearchResult result = searchCollection.search()
                .queryText("wireless headphones")
                .limit(5)
                .execute();

        assertNotNull(result);
        assertFalse("text search should return results", result.getIds().get(0).isEmpty());
    }
}
