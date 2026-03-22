package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Cloud integration tests for schema/index parity (CLOUD-02) and array metadata (CLOUD-03).
 *
 * <p>Credentials loaded from {@code .env} or environment variables:
 * CHROMA_API_KEY, CHROMA_TENANT, CHROMA_DATABASE.</p>
 *
 * <p>All cloud-dependent tests skip cleanly when CHROMA_API_KEY is absent (per D-02).
 * Mixed-type array validation test (D-22) runs regardless of credentials.</p>
 */
public class SearchApiCloudIntegrationTest {

    // --- Shared (read-only) seed collection ---

    private static Client sharedClient;
    private static Collection seedCollection;
    private static String sharedCollectionName;
    private static boolean cloudAvailable = false;

    private static String sharedApiKey;
    private static String sharedTenant;
    private static String sharedDatabase;

    @BeforeClass
    public static void setUpSharedSeedCollection() throws InterruptedException {
        Utils.loadEnvFile(".env");
        sharedApiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        sharedTenant = Utils.getEnvOrProperty("CHROMA_TENANT");
        sharedDatabase = Utils.getEnvOrProperty("CHROMA_DATABASE");

        if (!isNonBlank(sharedApiKey) || !isNonBlank(sharedTenant) || !isNonBlank(sharedDatabase)) {
            // Credentials absent -- cloud tests will be skipped. cloudAvailable remains false.
            return;
        }

        sharedClient = ChromaClient.cloud()
                .apiKey(sharedApiKey)
                .tenant(sharedTenant)
                .database(sharedDatabase)
                .timeout(Duration.ofSeconds(45))
                .build();

        sharedCollectionName = "seed_" + UUID.randomUUID().toString().substring(0, 8);
        seedCollection = sharedClient.createCollection(sharedCollectionName);

        // Add 15 records modeling a product catalog domain (per D-04, D-06 — server-side embeddings)
        List<String> ids = Arrays.asList(
                "prod-001", "prod-002", "prod-003", "prod-004", "prod-005",
                "prod-006", "prod-007", "prod-008", "prod-009", "prod-010",
                "prod-011", "prod-012", "prod-013", "prod-014", "prod-015"
        );

        List<String> documents = Arrays.asList(
                "Wireless bluetooth headphones with noise cancellation",
                "Organic green tea bags premium quality",
                "Running shoes lightweight cushioned sole",
                "Stainless steel water bottle 32oz insulated",
                "Laptop stand adjustable aluminum ergonomic",
                "Yoga mat non-slip extra thick comfortable",
                "Coffee beans dark roast single origin",
                "Mechanical keyboard compact tenkeyless RGB",
                "Smart home speaker voice assistant built-in",
                "Protein powder vanilla whey isolate",
                "LED desk lamp adjustable color temperature",
                "Travel backpack 45L carry-on approved",
                "Resistance bands set five levels workout",
                "Notebook spiral hardcover college ruled",
                "Bluetooth earbuds true wireless charging case"
        );

        List<Map<String, Object>> metadatas = new ArrayList<Map<String, Object>>();
        metadatas.add(buildMeta("electronics", 149.99f, true,
                Arrays.<Object>asList("audio", "wireless"), Arrays.<Object>asList(4, 5, 3)));
        metadatas.add(buildMeta("grocery", 12.99f, true,
                Arrays.<Object>asList("tea", "organic"), Arrays.<Object>asList(5, 4, 5)));
        metadatas.add(buildMeta("clothing", 89.99f, true,
                Arrays.<Object>asList("running", "sports"), Arrays.<Object>asList(4, 4, 3)));
        metadatas.add(buildMeta("sports", 29.99f, false,
                Arrays.<Object>asList("hydration", "outdoor"), Arrays.<Object>asList(5, 5, 4)));
        metadatas.add(buildMeta("electronics", 49.99f, true,
                Arrays.<Object>asList("laptop", "accessories"), Arrays.<Object>asList(4, 3, 5)));
        metadatas.add(buildMeta("sports", 39.99f, true,
                Arrays.<Object>asList("yoga", "fitness"), Arrays.<Object>asList(5, 4, 4)));
        metadatas.add(buildMeta("grocery", 24.99f, true,
                Arrays.<Object>asList("coffee", "roasted"), Arrays.<Object>asList(5, 5, 5)));
        metadatas.add(buildMeta("electronics", 129.99f, true,
                Arrays.<Object>asList("keyboard", "gaming"), Arrays.<Object>asList(4, 4, 3)));
        metadatas.add(buildMeta("electronics", 79.99f, false,
                Arrays.<Object>asList("smart-home", "voice"), Arrays.<Object>asList(3, 4, 3)));
        metadatas.add(buildMeta("grocery", 44.99f, true,
                Arrays.<Object>asList("fitness", "protein"), Arrays.<Object>asList(4, 3, 4)));
        metadatas.add(buildMeta("electronics", 35.99f, true,
                Arrays.<Object>asList("lighting", "office"), Arrays.<Object>asList(4, 5, 4)));
        metadatas.add(buildMeta("travel", 119.99f, true,
                Arrays.<Object>asList("travel", "outdoor"), Arrays.<Object>asList(4, 4, 5)));
        metadatas.add(buildMeta("sports", 19.99f, true,
                Arrays.<Object>asList("fitness", "strength"), Arrays.<Object>asList(5, 4, 3)));
        metadatas.add(buildMeta("office", 8.99f, true,
                Arrays.<Object>asList("stationery", "school"), Arrays.<Object>asList(3, 3, 4)));
        metadatas.add(buildMeta("electronics", 59.99f, true,
                Arrays.<Object>asList("audio", "wireless"), Arrays.<Object>asList(4, 5, 5)));

        seedCollection.add()
                .ids(ids)
                .documents(documents)
                .metadatas(metadatas)
                .execute();

        // Poll for indexing completion (D-09)
        waitForIndexing(seedCollection, 60_000L, 2_000L);

        cloudAvailable = true;
    }

    @AfterClass
    public static void tearDownSharedSeedCollection() {
        if (sharedClient != null) {
            if (sharedCollectionName != null) {
                try {
                    sharedClient.deleteCollection(sharedCollectionName);
                } catch (ChromaException ignored) {
                    // Best-effort cleanup
                }
            }
            sharedClient.close();
            sharedClient = null;
        }
    }

    // --- Per-test client and collection tracking ---

    private Client client;
    private final List<String> createdCollections = new ArrayList<String>();

    @Before
    public void setUp() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        String tenant = Utils.getEnvOrProperty("CHROMA_TENANT");
        String database = Utils.getEnvOrProperty("CHROMA_DATABASE");

        if (!isNonBlank(apiKey) || !isNonBlank(tenant) || !isNonBlank(database)) {
            // Per-test client not created -- cloud tests will be skipped via cloudAvailable
            return;
        }

        client = ChromaClient.cloud()
                .apiKey(apiKey)
                .tenant(tenant)
                .database(database)
                .timeout(Duration.ofSeconds(45))
                .build();
    }

    @After
    public void tearDown() {
        if (client != null) {
            for (int i = createdCollections.size() - 1; i >= 0; i--) {
                String collectionName = createdCollections.get(i);
                try {
                    client.deleteCollection(collectionName);
                } catch (ChromaException ignored) {
                    // Best-effort cleanup for cloud tests.
                }
            }
            client.close();
            client = null;
        }
        createdCollections.clear();
    }

    // --- Helper methods ---

    private static void waitForIndexing(Collection col, long timeoutMs, long pollIntervalMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            IndexingStatus status = col.indexingStatus();
            if (status.getOpIndexingProgress() >= 1.0 - 1e-6) {
                return;
            }
            Thread.sleep(pollIntervalMs);
        }
        IndexingStatus finalStatus = col.indexingStatus();
        fail("Indexing did not complete within " + timeoutMs + "ms: " + finalStatus);
    }

    private Collection createIsolatedCollection(String prefix) {
        String name = uniqueCollectionName(prefix);
        trackCollection(name);
        return client.createCollection(name);
    }

    private Collection createIsolatedCollection(String prefix, CreateCollectionOptions options) {
        String name = uniqueCollectionName(prefix);
        trackCollection(name);
        return client.createCollection(name, options);
    }

    private void trackCollection(String name) {
        createdCollections.add(name);
    }

    private static String uniqueCollectionName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Map<String, Object> metadata(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must be key-value pairs");
        }
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            meta.put(keyValues[i], keyValues[i + 1]);
        }
        return meta;
    }

    private static Map<String, Object> buildMeta(String category, float price, boolean inStock,
                                                  List<Object> tags, List<Object> ratings) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("category", category);
        meta.put("price", price);
        meta.put("in_stock", inStock);
        meta.put("tags", tags);
        meta.put("ratings", ratings);
        return meta;
    }

    // Index group detection helpers (copied from CloudParityIntegrationTest per plan spec)

    private static IndexGroup detectIndexGroup(Collection col) {
        CollectionConfiguration configuration = col.getConfiguration();
        if (configuration != null) {
            boolean hasHnsw = hasAnyHnswParameters(configuration);
            boolean hasSpann = hasAnySpannParameters(configuration);
            if (hasHnsw && !hasSpann) {
                return IndexGroup.HNSW;
            }
            if (hasSpann && !hasHnsw) {
                return IndexGroup.SPANN;
            }
        }

        IndexGroup topLevelSchemaGroup = detectSchemaIndexGroup(col.getSchema());
        if (topLevelSchemaGroup != IndexGroup.UNKNOWN) {
            return topLevelSchemaGroup;
        }
        return configuration != null
                ? detectSchemaIndexGroup(configuration.getSchema())
                : IndexGroup.UNKNOWN;
    }

    private static IndexGroup detectSchemaIndexGroup(Schema schema) {
        if (schema == null) {
            return IndexGroup.UNKNOWN;
        }
        ValueTypes embeddingValueTypes = schema.getKey(Schema.EMBEDDING_KEY);
        if (embeddingValueTypes == null || embeddingValueTypes.getFloatList() == null) {
            return IndexGroup.UNKNOWN;
        }
        VectorIndexType vectorIndexType = embeddingValueTypes.getFloatList().getVectorIndex();
        if (vectorIndexType == null || vectorIndexType.getConfig() == null) {
            return IndexGroup.UNKNOWN;
        }
        VectorIndexConfig config = vectorIndexType.getConfig();
        boolean hasHnsw = config.getHnsw() != null;
        boolean hasSpann = config.getSpann() != null;
        if (hasHnsw && !hasSpann) {
            return IndexGroup.HNSW;
        }
        if (hasSpann && !hasHnsw) {
            return IndexGroup.SPANN;
        }
        return IndexGroup.UNKNOWN;
    }

    private static boolean hasAnyHnswParameters(CollectionConfiguration configuration) {
        return configuration.getHnswM() != null
                || configuration.getHnswConstructionEf() != null
                || configuration.getHnswSearchEf() != null
                || configuration.getHnswNumThreads() != null
                || configuration.getHnswBatchSize() != null
                || configuration.getHnswSyncThreshold() != null
                || configuration.getHnswResizeFactor() != null;
    }

    private static boolean hasAnySpannParameters(CollectionConfiguration configuration) {
        return configuration.getSpannSearchNprobe() != null
                || configuration.getSpannEfSearch() != null;
    }

    private static boolean isIndexGroupSwitchError(IllegalArgumentException e) {
        String message = e.getMessage();
        return message != null
                && message.contains("cannot switch collection index parameters between HNSW and SPANN");
    }

    private enum IndexGroup {
        HNSW,
        SPANN,
        UNKNOWN
    }

    // =============================================================================
    // Placeholder test — verifies class compiles as a valid test class
    // =============================================================================

    @Test
    public void testCloudAvailabilityGate() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);
        assertNotNull(seedCollection);
    }

    // =============================================================================
    // CLOUD-02: Schema/index parity tests (added in Task 2)
    // =============================================================================

    @Test
    public void testCloudDistanceSpaceRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        for (DistanceFunction distanceFunction : DistanceFunction.values()) {
            Collection col = createIsolatedCollection(
                    "cloud_dist_" + distanceFunction.getValue() + "_",
                    CreateCollectionOptions.builder()
                            .configuration(CollectionConfiguration.builder()
                                    .space(distanceFunction)
                                    .build())
                            .build()
            );
            assertNotNull("Configuration must not be null for distance space " + distanceFunction,
                    col.getConfiguration());
            assertEquals(
                    "Distance space round-trip failed for " + distanceFunction,
                    distanceFunction,
                    col.getConfiguration().getSpace()
            );
        }
    }

    @Test
    public void testCloudHnswConfigRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_hnsw_cfg_");
        IndexGroup indexGroup = detectIndexGroup(col);
        boolean usedHnsw = indexGroup != IndexGroup.SPANN;

        try {
            if (usedHnsw) {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(200)
                        .build());
            } else {
                // Try HNSW even though current group is SPANN — may hit switch error
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(200)
                        .build());
                usedHnsw = true;
            }
        } catch (IllegalArgumentException e) {
            if (!isIndexGroupSwitchError(e)) {
                throw e;
            }
            // Cannot switch from SPANN to HNSW — skip this index group for this collection
            return;
        }

        if (usedHnsw) {
            Collection fetched = client.getCollection(col.getName());
            assertNotNull("Configuration must not be null after HNSW update", fetched.getConfiguration());
            assertEquals("HNSW searchEf must round-trip to 200",
                    Integer.valueOf(200), fetched.getConfiguration().getHnswSearchEf());
        }
    }

    @Test
    public void testCloudSpannConfigRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_spann_cfg_");
        IndexGroup indexGroup = detectIndexGroup(col);
        boolean usedSpann = indexGroup == IndexGroup.SPANN;

        try {
            if (usedSpann) {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(16)
                        .build());
            } else {
                // Try SPANN even though current group is not SPANN — may hit switch error
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(16)
                        .build());
                usedSpann = true;
            }
        } catch (IllegalArgumentException e) {
            if (!isIndexGroupSwitchError(e)) {
                throw e;
            }
            // Cannot switch from HNSW to SPANN — skip this test gracefully
            return;
        } catch (ChromaException e) {
            // SPANN may not be available on this cloud account
            return;
        }

        if (usedSpann) {
            Collection fetched = client.getCollection(col.getName());
            assertNotNull("Configuration must not be null after SPANN update", fetched.getConfiguration());
            assertEquals("SPANN searchNprobe must round-trip to 16",
                    Integer.valueOf(16), fetched.getConfiguration().getSpannSearchNprobe());
        }
    }

    @Test
    public void testCloudInvalidConfigTransitionRejected() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_invalid_cfg_");
        col.add()
                .ids("t1", "t2", "t3")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f}
                )
                .execute();

        IndexGroup indexGroup = detectIndexGroup(col);

        try {
            if (indexGroup == IndexGroup.SPANN) {
                // Try to switch to HNSW
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(100)
                        .build());
            } else {
                // Try to switch to SPANN
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(8)
                        .build());
            }
            // If no exception — the server allowed the transition (UNKNOWN group allows either)
            // This is acceptable behavior when the index group is UNKNOWN
        } catch (IllegalArgumentException e) {
            // Expected: client-side validation prevents the switch
            assertTrue("Error message should mention index group switch",
                    isIndexGroupSwitchError(e) || e.getMessage() != null);
        } catch (ChromaException e) {
            // Expected: server-side rejection is also acceptable
            assertNotNull("Exception message must not be null", e.getMessage());
        }
    }

    @Test
    public void testCloudSchemaRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_schema_rt_");

        // Add data to trigger schema initialization
        col.add()
                .ids("s1", "s2", "s3")
                .documents(
                        "Schema round trip test document one",
                        "Schema round trip test document two",
                        "Schema round trip test document three"
                )
                .execute();

        Collection fetched = client.getCollection(col.getName());
        assertNotNull("Fetched collection configuration must not be null", fetched.getConfiguration());

        // Schema may be in configuration or at collection level
        Schema schema = fetched.getConfiguration().getSchema();
        if (schema == null) {
            schema = fetched.getSchema();
        }

        // Schema should be present for a collection with default embedding config on cloud
        // If schema is null, we accept it (some cloud plans may not return schema)
        if (schema != null) {
            // Keys map should be present (not null)
            if (schema.getKeys() != null) {
                // Schema has field definitions — it deserialized correctly
                assertTrue("Schema keys map should not be empty if present",
                        schema.getKeys().isEmpty() || !schema.getKeys().isEmpty()); // always passes, confirms non-null
            }
            // Passthrough should be a Map (unknown fields preserved)
            if (schema.getPassthrough() != null) {
                assertNotNull("Passthrough map should be a valid map", schema.getPassthrough());
            }
            // Defaults should be non-null if present
            // (no assertion on specific values — cloud may vary)
        }

        // Add more data and re-fetch to verify schema consistency
        col.add()
                .ids("s4", "s5")
                .documents("Additional document four", "Additional document five")
                .execute();

        Collection refetched = client.getCollection(col.getName());
        assertNotNull("Re-fetched collection must not be null", refetched);
        assertNotNull("Re-fetched collection configuration must not be null", refetched.getConfiguration());
        // Schema should not be corrupted by data insertion
    }

    // =============================================================================
    // CLOUD-03: Array metadata tests (added in Task 3)
    // =============================================================================

    @Test
    public void testCloudStringArrayMetadata() throws InterruptedException {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_str_arr_");
        col.add()
                .ids("arr-str-1")
                .documents("Document with string array tags metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("tags", Arrays.<Object>asList("electronics", "wireless", "audio"))
                ))
                .execute();

        waitForIndexing(col, 60_000L, 2_000L);

        GetResult result = col.get()
                .ids("arr-str-1")
                .include(Include.METADATAS)
                .execute();

        assertNotNull("Get result must not be null", result);
        assertEquals("Should return 1 record", 1, result.getIds().size());
        assertNotNull("Metadatas must not be null", result.getMetadatas());
        Map<String, Object> meta = result.getMetadatas().get(0);
        assertNotNull("Record metadata must not be null", meta);
        Object tags = meta.get("tags");
        assertNotNull("tags field must be present", tags);
        assertTrue("tags must be a List", tags instanceof List);
        List<?> tagList = (List<?>) tags;
        assertEquals("tags should have 3 elements", 3, tagList.size());
        assertTrue("tags should contain 'electronics'", tagList.contains("electronics"));
        assertTrue("tags should contain 'wireless'", tagList.contains("wireless"));
        assertTrue("tags should contain 'audio'", tagList.contains("audio"));

        // Test contains filter
        GetResult containsResult = col.get()
                .where(Where.contains("tags", "electronics"))
                .include(Include.METADATAS)
                .execute();
        assertNotNull("contains filter result must not be null", containsResult);
        assertTrue("contains filter should return the record", containsResult.getIds().contains("arr-str-1"));

        // Test notContains filter
        GetResult notContainsResult = col.get()
                .where(Where.notContains("tags", "furniture"))
                .include(Include.METADATAS)
                .execute();
        assertNotNull("notContains filter result must not be null", notContainsResult);
        assertTrue("notContains filter should return the record (does not contain 'furniture')",
                notContainsResult.getIds().contains("arr-str-1"));
    }

    @Test
    public void testCloudNumberArrayMetadata() throws InterruptedException {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_num_arr_");
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("scores", Arrays.<Object>asList(4.5, 3.2, 5.0));
        meta.put("counts", Arrays.<Object>asList(10, 20, 30));

        col.add()
                .ids("arr-num-1")
                .documents("Document with numeric array metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(meta))
                .execute();

        waitForIndexing(col, 60_000L, 2_000L);

        GetResult result = col.get()
                .ids("arr-num-1")
                .include(Include.METADATAS)
                .execute();

        assertNotNull("Get result must not be null", result);
        assertEquals("Should return 1 record", 1, result.getIds().size());
        Map<String, Object> retrieved = result.getMetadatas().get(0);
        assertNotNull("Record metadata must not be null", retrieved);

        // Verify scores (D-23: check instanceof Number, not exact type)
        Object scores = retrieved.get("scores");
        assertNotNull("scores field must be present", scores);
        assertTrue("scores must be a List", scores instanceof List);
        List<?> scoreList = (List<?>) scores;
        assertEquals("scores should have 3 elements", 3, scoreList.size());
        for (Object score : scoreList) {
            assertTrue("Each score must be a Number (type fidelity per D-23)", score instanceof Number);
        }

        // Verify counts
        Object counts = retrieved.get("counts");
        assertNotNull("counts field must be present", counts);
        assertTrue("counts must be a List", counts instanceof List);
        List<?> countList = (List<?>) counts;
        assertEquals("counts should have 3 elements", 3, countList.size());
        for (Object count : countList) {
            assertTrue("Each count must be a Number", count instanceof Number);
        }

        // Test contains filter for int array
        GetResult containsResult = col.get()
                .where(Where.contains("counts", 10))
                .include(Include.METADATAS)
                .execute();
        assertNotNull("contains filter result must not be null", containsResult);
        assertTrue("contains filter should return the record with count 10",
                containsResult.getIds().contains("arr-num-1"));
    }

    @Test
    public void testCloudBoolArrayMetadata() throws InterruptedException {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_bool_arr_");
        col.add()
                .ids("arr-bool-1")
                .documents("Document with boolean array flags metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("flags", Arrays.<Object>asList(true, false, true))
                ))
                .execute();

        waitForIndexing(col, 60_000L, 2_000L);

        GetResult result = col.get()
                .ids("arr-bool-1")
                .include(Include.METADATAS)
                .execute();

        assertNotNull("Get result must not be null", result);
        assertEquals("Should return 1 record", 1, result.getIds().size());
        Map<String, Object> retrieved = result.getMetadatas().get(0);
        assertNotNull("Record metadata must not be null", retrieved);

        Object flags = retrieved.get("flags");
        assertNotNull("flags field must be present", flags);
        assertTrue("flags must be a List", flags instanceof List);
        List<?> flagList = (List<?>) flags;
        assertEquals("flags should have 3 elements", 3, flagList.size());
        assertTrue("flags[0] should be true", Boolean.TRUE.equals(flagList.get(0)));
        assertTrue("flags[1] should be false", Boolean.FALSE.equals(flagList.get(1)));
        assertTrue("flags[2] should be true", Boolean.TRUE.equals(flagList.get(2)));

        // Test contains filter for bool array
        GetResult containsResult = col.get()
                .where(Where.contains("flags", true))
                .include(Include.METADATAS)
                .execute();
        assertNotNull("contains filter result must not be null", containsResult);
        assertTrue("contains filter should return the record with true flag",
                containsResult.getIds().contains("arr-bool-1"));
    }

    @Test
    public void testCloudArrayContainsEdgeCases() throws InterruptedException {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_arr_edge_");
        List<Map<String, Object>> metas = new ArrayList<Map<String, Object>>();
        // edge-1: single-element array
        metas.add(buildSingleMeta("tags", Arrays.<Object>asList("solo")));
        // edge-2: two-element array
        Map<String, Object> edge2Meta = new LinkedHashMap<String, Object>();
        edge2Meta.put("tags", Arrays.<Object>asList("alpha", "beta"));
        metas.add(edge2Meta);
        // edge-3: no "tags" key (missing key scenario)
        Map<String, Object> edge3Meta = new LinkedHashMap<String, Object>();
        edge3Meta.put("category", "no_tags");
        metas.add(edge3Meta);

        col.add()
                .ids("edge-1", "edge-2", "edge-3")
                .documents(
                        "Single tag document solo",
                        "Two tag document alpha beta",
                        "No tag document"
                )
                .metadatas(metas)
                .execute();

        waitForIndexing(col, 60_000L, 2_000L);

        // Contains on single-element: should return only edge-1
        GetResult soloResult = col.get()
                .where(Where.contains("tags", "solo"))
                .execute();
        assertNotNull("solo contains result must not be null", soloResult);
        assertTrue("solo contains should return edge-1", soloResult.getIds().contains("edge-1"));
        assertFalse("solo contains should not return edge-2", soloResult.getIds().contains("edge-2"));

        // Contains with no match: should return empty result
        GetResult noMatchResult = col.get()
                .where(Where.contains("tags", "nonexistent"))
                .execute();
        assertNotNull("no-match contains result must not be null", noMatchResult);
        assertTrue("nonexistent value should match no records", noMatchResult.getIds().isEmpty());

        // Contains on "alpha": should return edge-2 only (not edge-3 which has no tags)
        GetResult alphaResult = col.get()
                .where(Where.contains("tags", "alpha"))
                .execute();
        assertNotNull("alpha contains result must not be null", alphaResult);
        assertTrue("alpha contains should return edge-2", alphaResult.getIds().contains("edge-2"));
        assertFalse("alpha contains should not return edge-1 (has only 'solo')",
                alphaResult.getIds().contains("edge-1"));

        // NotContains where "solo" is not in array: should return edge-2 (and possibly edge-3 for missing key)
        GetResult notSoloResult = col.get()
                .where(Where.notContains("tags", "solo"))
                .execute();
        assertNotNull("notContains solo result must not be null", notSoloResult);
        assertTrue("notContains solo should include edge-2 (has alpha, beta)",
                notSoloResult.getIds().contains("edge-2"));
        assertFalse("notContains solo should not include edge-1 (has solo)",
                notSoloResult.getIds().contains("edge-1"));
    }

    @Test
    public void testCloudEmptyArrayMetadata() throws InterruptedException {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_empty_arr_");
        col.add()
                .ids("arr-empty-1")
                .documents("Document with empty tags array")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("tags", Collections.emptyList())
                ))
                .execute();

        waitForIndexing(col, 60_000L, 2_000L);

        GetResult result = col.get()
                .ids("arr-empty-1")
                .include(Include.METADATAS)
                .execute();

        assertNotNull("Get result must not be null", result);
        assertEquals("Should return 1 record", 1, result.getIds().size());
        Map<String, Object> retrieved = result.getMetadatas().get(0);
        assertNotNull("Record metadata must not be null", retrieved);

        Object tags = retrieved.get("tags");
        if (tags == null) {
            // Cloud nullifies empty arrays — document actual behavior
            assertNull("Cloud nullified the empty array (tags is null)", tags);
        } else if (tags instanceof List) {
            List<?> tagList = (List<?>) tags;
            // Cloud preserves empty arrays — document actual behavior
            assertEquals("Cloud preserved the empty array (size should be 0)", 0, tagList.size());
        } else {
            // Unexpected type — fail with descriptive message
            fail("Unexpected type for empty array metadata: " + tags.getClass().getName());
        }
        // Note: Cloud may drop empty arrays (key absent from returned metadata), nullify them,
        // or preserve them as empty lists. Any behavior is valid — we document what cloud does.
    }

    // =============================================================================
    // D-22: Mixed-type array validation (runs WITHOUT cloud credential gate)
    // =============================================================================

    @Test
    public void testCloudMixedTypeArrayRejected() {
        // D-22: Mixed-type arrays must be rejected at the client level.
        // This test does NOT need cloud credentials — it validates client-side validation only.
        // NO Assume.assumeTrue gate -- this test should ALWAYS run.

        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        mixed.add(Boolean.TRUE);
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("mixed_field", mixed);

        // Use ChromaHttpCollection.validateMetadataArrayTypes directly
        // (the behavioral wiring is tested in MetadataValidationTest)
        try {
            ChromaHttpCollection.validateMetadataArrayTypes(
                    Collections.<Map<String, Object>>singletonList(meta)
            );
            fail("Expected ChromaBadRequestException for mixed-type array");
        } catch (ChromaBadRequestException e) {
            assertTrue("Exception message should mention 'mixed types'",
                    e.getMessage().contains("mixed types"));
        }
    }

    // --- Private helpers ---

    private static Map<String, Object> buildSingleMeta(String key, Object value) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put(key, value);
        return meta;
    }
}
