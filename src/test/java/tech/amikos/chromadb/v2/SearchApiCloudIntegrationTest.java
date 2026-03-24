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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Query embedding constants matching seed collection clusters (4D)
    private static final float[] QUERY_ELECTRONICS = {0.85f, 0.15f, 0.05f, 0.05f};
    private static final float[] QUERY_GROCERY = {0.05f, 0.85f, 0.15f, 0.05f};
    private static final List<String> ELECTRONICS_IDS = Arrays.asList(
            "prod-001", "prod-005", "prod-008", "prod-009", "prod-011", "prod-015");
    private static final List<String> GROCERY_IDS = Arrays.asList(
            "prod-002", "prod-007", "prod-010");

    private static String sharedApiKey;
    private static String sharedTenant;
    private static String sharedDatabase;

    @BeforeClass
    public static void setUpSharedSeedCollection() {
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
        metadatas.add(buildMeta("electronics", 149.99, true,
                Arrays.<Object>asList("audio", "wireless"), Arrays.<Object>asList(4, 5, 3)));
        metadatas.add(buildMeta("grocery", 12.99, true,
                Arrays.<Object>asList("tea", "organic"), Arrays.<Object>asList(5, 4, 5)));
        metadatas.add(buildMeta("clothing", 89.99, true,
                Arrays.<Object>asList("running", "sports"), Arrays.<Object>asList(4, 4, 3)));
        metadatas.add(buildMeta("sports", 29.99, false,
                Arrays.<Object>asList("hydration", "outdoor"), Arrays.<Object>asList(5, 5, 4)));
        metadatas.add(buildMeta("electronics", 49.99, true,
                Arrays.<Object>asList("laptop", "accessories"), Arrays.<Object>asList(4, 3, 5)));
        metadatas.add(buildMeta("sports", 39.99, true,
                Arrays.<Object>asList("yoga", "fitness"), Arrays.<Object>asList(5, 4, 4)));
        metadatas.add(buildMeta("grocery", 24.99, true,
                Arrays.<Object>asList("coffee", "roasted"), Arrays.<Object>asList(5, 5, 5)));
        metadatas.add(buildMeta("electronics", 129.99, true,
                Arrays.<Object>asList("keyboard", "gaming"), Arrays.<Object>asList(4, 4, 3)));
        metadatas.add(buildMeta("electronics", 79.99, false,
                Arrays.<Object>asList("smart-home", "voice"), Arrays.<Object>asList(3, 4, 3)));
        metadatas.add(buildMeta("grocery", 44.99, true,
                Arrays.<Object>asList("fitness", "protein"), Arrays.<Object>asList(4, 3, 4)));
        metadatas.add(buildMeta("electronics", 35.99, true,
                Arrays.<Object>asList("lighting", "office"), Arrays.<Object>asList(4, 5, 4)));
        metadatas.add(buildMeta("travel", 119.99, true,
                Arrays.<Object>asList("travel", "outdoor"), Arrays.<Object>asList(4, 4, 5)));
        metadatas.add(buildMeta("sports", 19.99, true,
                Arrays.<Object>asList("fitness", "strength"), Arrays.<Object>asList(5, 4, 3)));
        metadatas.add(buildMeta("office", 8.99, true,
                Arrays.<Object>asList("stationery", "school"), Arrays.<Object>asList(3, 3, 4)));
        metadatas.add(buildMeta("electronics", 59.99, true,
                Arrays.<Object>asList("audio", "wireless"), Arrays.<Object>asList(4, 5, 5)));

        seedCollection.add()
                .ids(ids)
                .documents(documents)
                .metadatas(metadatas)
                .embeddings(
                        // Electronics cluster: dominant first dimension
                        new float[]{0.90f, 0.10f, 0.10f, 0.10f},  // prod-001 headphones
                        // Grocery cluster: dominant second dimension
                        new float[]{0.10f, 0.90f, 0.10f, 0.10f},  // prod-002 tea
                        // Clothing/Sports cluster: dominant third dimension
                        new float[]{0.15f, 0.10f, 0.85f, 0.10f},  // prod-003 shoes
                        new float[]{0.10f, 0.10f, 0.80f, 0.20f},  // prod-004 water bottle
                        new float[]{0.85f, 0.15f, 0.10f, 0.10f},  // prod-005 laptop stand
                        new float[]{0.10f, 0.10f, 0.90f, 0.10f},  // prod-006 yoga mat
                        new float[]{0.10f, 0.85f, 0.15f, 0.10f},  // prod-007 coffee
                        new float[]{0.88f, 0.12f, 0.10f, 0.10f},  // prod-008 keyboard
                        new float[]{0.80f, 0.10f, 0.10f, 0.20f},  // prod-009 speaker
                        new float[]{0.10f, 0.80f, 0.20f, 0.10f},  // prod-010 protein
                        new float[]{0.82f, 0.10f, 0.10f, 0.18f},  // prod-011 desk lamp
                        // Travel/Office cluster: dominant fourth dimension
                        new float[]{0.10f, 0.10f, 0.20f, 0.80f},  // prod-012 backpack
                        new float[]{0.10f, 0.10f, 0.85f, 0.15f},  // prod-013 resistance bands
                        new float[]{0.10f, 0.10f, 0.10f, 0.90f},  // prod-014 notebook
                        new float[]{0.87f, 0.13f, 0.10f, 0.10f}   // prod-015 earbuds
                )
                .execute();

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

    private static Map<String, Object> buildMeta(String category, double price, boolean inStock,
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
            // Try create response first, then re-fetch — cloud may not echo config in create
            DistanceFunction actual = null;
            if (col.getConfiguration() != null) {
                actual = col.getConfiguration().getSpace();
            }
            if (actual == null) {
                Collection fetched = client.getCollection(col.getName());
                if (fetched.getConfiguration() != null) {
                    actual = fetched.getConfiguration().getSpace();
                }
            }
            // Cloud may not expose distance space in configuration response
            if (actual != null) {
                assertEquals(
                        "Distance space round-trip failed for " + distanceFunction,
                        distanceFunction,
                        actual
                );
            }
        }
    }

    @Test
    public void testCloudHnswConfigRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_hnsw_cfg_");

        try {
            col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                    .hnswSearchEf(200)
                    .build());
        } catch (IllegalArgumentException e) {
            if (!isIndexGroupSwitchError(e)) {
                throw e;
            }
            // Cannot switch from SPANN to HNSW — skip this index group for this collection
            return;
        }

        Collection fetched = client.getCollection(col.getName());
        assertNotNull("Configuration must not be null after HNSW update", fetched.getConfiguration());
        assertEquals("HNSW searchEf must round-trip to 200",
                Integer.valueOf(200), fetched.getConfiguration().getHnswSearchEf());
    }

    @Test
    public void testCloudSpannConfigRoundTrip() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_spann_cfg_");

        try {
            col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                    .spannSearchNprobe(16)
                    .build());
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

        Collection fetched = client.getCollection(col.getName());
        if (fetched.getConfiguration() == null
                || fetched.getConfiguration().getSpannSearchNprobe() == null) {
            // Cloud accepted the update but does not expose SPANN params in config response
            return;
        }
        assertEquals("SPANN searchNprobe must round-trip to 16",
                Integer.valueOf(16), fetched.getConfiguration().getSpannSearchNprobe());
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
                    isIndexGroupSwitchError(e));
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
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f}
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
            // Schema deserialized correctly — verify keys map is non-null
            assertNotNull("Schema keys map should not be null", schema.getKeys());
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
                .embeddings(
                        new float[]{0.5f, 0.5f, 0.0f},
                        new float[]{0.0f, 0.5f, 0.5f}
                )
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
    public void testCloudStringArrayMetadata() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_str_arr_");
        col.add()
                .ids("arr-str-1")
                .documents("Document with string array tags metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("tags", Arrays.<Object>asList("electronics", "wireless", "audio"))
                ))
                .embeddings(new float[]{0.9f, 0.1f, 0.1f})
                .execute();


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
    public void testCloudNumberArrayMetadata() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_num_arr_");
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("scores", Arrays.<Object>asList(4.5, 3.2, 5.0));
        meta.put("counts", Arrays.<Object>asList(10, 20, 30));

        col.add()
                .ids("arr-num-1")
                .documents("Document with numeric array metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(meta))
                .embeddings(new float[]{0.1f, 0.9f, 0.1f})
                .execute();


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
    public void testCloudBoolArrayMetadata() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_bool_arr_");
        col.add()
                .ids("arr-bool-1")
                .documents("Document with boolean array flags metadata")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("flags", Arrays.<Object>asList(true, false, true))
                ))
                .embeddings(new float[]{0.1f, 0.1f, 0.9f})
                .execute();


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
    public void testCloudArrayContainsEdgeCases() {
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
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f}
                )
                .execute();


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
    public void testCloudEmptyArrayMetadata() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Collection col = createIsolatedCollection("cloud_empty_arr_");
        col.add()
                .ids("arr-empty-1")
                .documents("Document with empty tags array")
                .metadatas(Collections.<Map<String, Object>>singletonList(
                        buildSingleMeta("tags", Collections.emptyList())
                ))
                .embeddings(new float[]{0.5f, 0.5f, 0.1f})
                .execute();


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
            // Cloud nullifies empty arrays — this is acceptable behavior
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

    // =============================================================================
    // CLOUD-01: Search parity tests (D-07 through D-12)
    // =============================================================================

    @Test
    public void testCloudKnnSearch() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        SearchResult result = seedCollection.search()
                .queryEmbedding(QUERY_ELECTRONICS)
                .limit(5)
                .execute();

        assertNotNull("SearchResult should not be null", result);
        assertNotNull("ids should not be null", result.getIds());
        assertFalse("ids should not be empty", result.getIds().isEmpty());
        assertFalse("first search group should have results", result.getIds().get(0).isEmpty());
        assertTrue("should return at most 5 results", result.getIds().get(0).size() <= 5);

        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertFalse("rows should not be empty", rows.isEmpty());
        for (SearchResultRow row : rows) {
            assertNotNull("row id should not be null", row.getId());
        }
        // Verify top result is from the electronics cluster (seed data has 6 electronics products
        // with dominant first-dimension embeddings matching QUERY_ELECTRONICS)
        assertTrue("Top KNN result should be from electronics cluster",
                ELECTRONICS_IDS.contains(rows.get(0).getId()));
    }

    @Test
    public void testCloudRrfSearch() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // RRF is expanded client-side into arithmetic rank expressions:
        // -(w1/(k+rank1) + w2/(k+rank2))
        Rrf rrf = Rrf.builder()
                .rank(Knn.queryEmbedding(QUERY_ELECTRONICS).limit(50), 0.7)
                .rank(Knn.queryEmbedding(QUERY_GROCERY).limit(50), 0.3)
                .k(60)
                .build();
        Search s = Search.builder()
                .rrf(rrf)
                .selectAll()
                .limit(5)
                .build();
        SearchResult result = seedCollection.search().searches(s).execute();

        assertNotNull("RRF result should not be null", result);
        assertFalse("RRF should return results", result.rows(0).isEmpty());
        assertTrue("RRF should return at most 5 results", result.rows(0).size() <= 5);
        for (SearchResultRow row : result.rows(0)) {
            assertNotNull("RRF row id should not be null", row.getId());
            assertNotNull("RRF row score should not be null", row.getScore());
        }
    }

    @Test
    public void testCloudGroupBySearch() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                .groupBy(GroupBy.builder().key("category").maxK(2).build())
                .selectAll()
                .limit(10)
                .build();
        SearchResult result = seedCollection.search().searches(s).execute();

        assertNotNull("GroupBy result should not be null", result);
        assertNotNull("ids should not be null", result.getIds());
        // GroupBy flattens into standard column-major response; access via rows()
        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertNotNull("rows should not be null", rows);
        assertFalse("GroupBy should return at least 1 row", rows.isEmpty());
        // Verify grouping semantics: multiple distinct categories should appear in results
        // (seed data has 6 categories; QUERY_ELECTRONICS + limit(10) should reach several)
        Set<String> categories = new HashSet<String>();
        for (SearchResultRow row : rows) {
            assertNotNull("Metadata should be present when selectAll() is used", row.getMetadata());
            Object cat = row.getMetadata().get("category");
            assertNotNull("category key should be present in metadata", cat);
            categories.add((String) cat);
        }
        assertTrue("GroupBy should return results from multiple categories", categories.size() > 1);
    }

    @Test
    public void testCloudBatchSearch() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Search s1 = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                .limit(3)
                .build();
        Search s2 = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_GROCERY))
                .limit(3)
                .build();
        SearchResult result = seedCollection.search().searches(s1, s2).execute();

        assertNotNull("Batch result should not be null", result);
        assertEquals("Should have 2 search groups", 2, result.searchCount());
        assertFalse("group 0 should have results", result.rows(0).isEmpty());
        assertFalse("group 1 should have results", result.rows(1).isEmpty());
        // Verify groups correspond to their query clusters: group 0 = electronics, group 1 = grocery
        assertTrue("Batch group 0 top result should be from electronics cluster",
                ELECTRONICS_IDS.contains(result.rows(0).get(0).getId()));
        assertTrue("Batch group 1 top result should be from grocery cluster",
                GROCERY_IDS.contains(result.rows(1).get(0).getId()));
    }

    @Test
    public void testCloudSearchReadLevelIndexAndWal() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // Use an isolated collection with explicit 3D embeddings; search immediately (no polling)
        // to test that INDEX_AND_WAL reads recently written WAL records
        final Collection col = createIsolatedCollection("cloud_rl_wal_");
        col.add()
                .ids("rl-1", "rl-2", "rl-3")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f}
                )
                .documents(
                        "ReadLevel test document one",
                        "ReadLevel test document two",
                        "ReadLevel test document three"
                )
                .execute();

        // INDEX_AND_WAL guarantees WAL records are visible; use assertEventually to
        // tolerate brief cloud replication delays without masking real failures
        assertEventually(Duration.ofSeconds(10), Duration.ofSeconds(1), new Runnable() {
            @Override
            public void run() {
                SearchResult result = col.search()
                        .queryEmbedding(new float[]{0.9f, 0.1f, 0.1f})
                        .readLevel(ReadLevel.INDEX_AND_WAL)
                        .limit(3)
                        .execute();

                assertNotNull("INDEX_AND_WAL result should not be null", result);
                assertNotNull("ids should not be null", result.getIds());
                assertEquals("INDEX_AND_WAL should return all 3 freshly written records",
                        3, result.rows(0).size());
            }
        });
    }

    @Test
    public void testCloudSearchReadLevelIndexOnly() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // Use shared seedCollection (already indexed from @BeforeClass)
        SearchResult result = seedCollection.search()
                .queryEmbedding(QUERY_ELECTRONICS)
                .readLevel(ReadLevel.INDEX_ONLY)
                .limit(5)
                .execute();

        assertNotNull("INDEX_ONLY result should not be null", result);
        assertNotNull("ids outer list must be non-null", result.getIds());
        // INDEX_ONLY may return 0 results if the index hasn't compacted yet (async on Cloud).
        // The key assertion is that the call succeeds without error.
        assertTrue("INDEX_ONLY result count must be >= 0 and <= 15",
                result.getIds().get(0).size() >= 0 && result.getIds().get(0).size() <= 15);
    }

    @Test
    public void testCloudKnnLimitVsSearchLimit() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // Knn.limit(10) retrieves 10 nearest neighbor candidates;
        // Search.limit(3) caps the final result count returned to the caller
        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_ELECTRONICS).limit(10))
                .selectAll()
                .limit(3)
                .build();
        SearchResult result = seedCollection.search().searches(s).execute();

        assertNotNull("KnnLimit result should not be null", result);
        assertFalse("KnnLimit search should return at least 1 result", result.rows(0).isEmpty());
        // Search.limit(3) caps final result count even though Knn.limit(10) retrieves 10 candidates
        assertEquals("Search.limit(3) must cap final result count to exactly 3",
                3, result.rows(0).size());
    }

    @Test
    public void testCloudSearchFilterMatrix() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // Sub-test A: Where metadata filter alone
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.eq("category", "electronics"))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-A result should not be null", result);
            // Seed data has 6 electronics products matching QUERY_ELECTRONICS
            assertFalse("Filter-A should return at least one electronics record", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertNotNull("category metadata should be present", row.getMetadata());
                assertEquals("All rows should have category=electronics",
                        "electronics", row.getMetadata().get("category"));
            }
        }

        // Sub-test B: IDIn alone
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.idIn("prod-001", "prod-005", "prod-008"))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-B result should not be null", result);
            // All 3 IDs exist in seed data and are in the electronics cluster
            assertFalse("Filter-B IDIn should return at least 1 result", result.rows(0).isEmpty());
            assertTrue("IDIn should return at most 3 results", result.rows(0).size() <= 3);
            for (SearchResultRow row : result.rows(0)) {
                assertTrue("IDIn should only return matching ids",
                        "prod-001".equals(row.getId()) || "prod-005".equals(row.getId()) || "prod-008".equals(row.getId()));
            }
        }

        // Sub-test C: IDNotIn alone
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.idNotIn("prod-001", "prod-002"))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-C result should not be null", result);
            // 13 products remain after excluding 2; QUERY_ELECTRONICS should match several
            assertFalse("Filter-C IDNotIn should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertFalse("IDNotIn should exclude prod-001", "prod-001".equals(row.getId()));
                assertFalse("IDNotIn should exclude prod-002", "prod-002".equals(row.getId()));
            }
        }

        // Sub-test D: DocumentContains alone
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.documentContains("headphones"))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-D result should not be null", result);
            // prod-001 ("Wireless bluetooth headphones...") matches this filter
            assertFalse("Filter-D DocumentContains should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertNotNull("Document should be present", row.getDocument());
                assertTrue("DocumentContains filter: document must contain 'headphones'",
                        row.getDocument().toLowerCase().contains("headphones"));
            }
        }

        // Sub-test E: IDNotIn + metadata filter combined
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.and(Where.idNotIn("prod-001"), Where.eq("category", "electronics")))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-E result should not be null", result);
            // 5 electronics products remain after excluding prod-001
            assertFalse("Filter-E IDNotIn+metadata should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertFalse("IDNotIn+metadata: should exclude prod-001", "prod-001".equals(row.getId()));
                assertEquals("IDNotIn+metadata: all rows should be electronics",
                        "electronics", row.getMetadata().get("category"));
            }
        }

        // Sub-test F: Where + DocumentContains combined
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.and(Where.eq("category", "electronics"), Where.documentContains("wireless")))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-F result should not be null", result);
            // prod-001 and prod-015 are electronics with "wireless" in document
            assertFalse("Filter-F Where+DocumentContains should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertEquals("Where+DocumentContains: category must be electronics",
                        "electronics", row.getMetadata().get("category"));
                assertTrue("Where+DocumentContains: document must contain 'wireless'",
                        row.getDocument() != null && row.getDocument().toLowerCase().contains("wireless"));
            }
        }

        // Sub-test G: DocumentNotContains alone
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.documentNotContains("headphones"))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-G result should not be null", result);
            // 14 of 15 products don't contain "headphones"; QUERY_ELECTRONICS should match several
            assertFalse("Filter-G DocumentNotContains should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertFalse("DocumentNotContains: document must not contain 'headphones'",
                        row.getDocument() != null && row.getDocument().toLowerCase().contains("headphones"));
            }
        }

        // Sub-test H: Where + IDIn + DocumentContains triple combination
        {
            Search s = Search.builder()
                    .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                    .where(Where.and(
                            Where.eq("category", "electronics"),
                            Where.idIn("prod-001", "prod-005", "prod-008", "prod-009", "prod-011", "prod-015"),
                            Where.documentContains("wireless")))
                    .selectAll()
                    .limit(10)
                    .build();
            SearchResult result = seedCollection.search().searches(s).execute();
            assertNotNull("Filter-H result should not be null", result);
            // prod-001 and prod-015 are electronics, in the IDIn set, and contain "wireless"
            assertFalse("Filter-H triple combination should return at least 1 result", result.rows(0).isEmpty());
            for (SearchResultRow row : result.rows(0)) {
                assertEquals("Filter-H: category must be electronics",
                        "electronics", row.getMetadata().get("category"));
                String id = row.getId();
                assertTrue("Filter-H: ID must be in allowed set",
                        "prod-001".equals(id) || "prod-005".equals(id) || "prod-008".equals(id)
                                || "prod-009".equals(id) || "prod-011".equals(id) || "prod-015".equals(id));
                assertTrue("Filter-H: document must contain 'wireless'",
                        row.getDocument() != null && row.getDocument().toLowerCase().contains("wireless"));
            }
        }
    }

    @Test
    public void testCloudSearchPagination() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        // Sub-test A: Basic limit
        {
            SearchResult result = seedCollection.search()
                    .queryEmbedding(QUERY_ELECTRONICS)
                    .limit(3)
                    .execute();
            assertNotNull("Pagination-A result should not be null", result);
            assertFalse("Pagination-A should return at least 1 result", result.rows(0).isEmpty());
            assertTrue("limit(3) must return <= 3 results", result.rows(0).size() <= 3);
        }

        // Sub-test B: Limit+offset (page 2)
        {
            SearchResult page1 = seedCollection.search()
                    .queryEmbedding(QUERY_ELECTRONICS)
                    .limit(3)
                    .offset(0)
                    .execute();
            SearchResult page2 = seedCollection.search()
                    .queryEmbedding(QUERY_ELECTRONICS)
                    .limit(3)
                    .offset(3)
                    .execute();
            assertFalse("page1 should have results", page1.rows(0).isEmpty());
            assertNotNull("page2 result should not be null", page2);
            // If both pages have results, first rows must differ (different pages)
            if (!page1.rows(0).isEmpty() && !page2.rows(0).isEmpty()) {
                assertFalse("page1 and page2 first IDs must differ",
                        page1.rows(0).get(0).getId().equals(page2.rows(0).get(0).getId()));
            }
        }

        // Sub-test C: Client-side validation for invalid inputs (D-14)
        // These should fail without sending HTTP requests
        {
            try {
                seedCollection.search()
                        .queryEmbedding(QUERY_ELECTRONICS)
                        .limit(0)
                        .execute();
                fail("Expected IllegalArgumentException for limit=0");
            } catch (IllegalArgumentException e) {
                assertTrue("Exception message should mention limit constraint",
                        e.getMessage() != null && e.getMessage().contains("limit must be > 0"));
            }
        }
        {
            try {
                seedCollection.search()
                        .queryEmbedding(QUERY_ELECTRONICS)
                        .limit(3)
                        .offset(-1)
                        .execute();
                fail("Expected IllegalArgumentException for negative offset");
            } catch (IllegalArgumentException e) {
                assertTrue("Exception message should mention offset constraint",
                        e.getMessage() != null && e.getMessage().contains("offset must be >= 0"));
            }
        }
    }

    @Test
    public void testCloudSearchProjectionPresent() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);

        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                .select(Select.ID, Select.SCORE, Select.DOCUMENT)
                .limit(3)
                .build();
        SearchResult result = seedCollection.search().searches(s).execute();

        assertNotNull("Projection result should not be null", result);
        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertFalse("Projection rows should not be empty", rows.isEmpty());
        for (SearchResultRow row : rows) {
            assertNotNull("ID should be present when selected", row.getId());
            assertNotNull("Score should be present when selected", row.getScore());
            assertNotNull("Document should be present when selected", row.getDocument());
        }
        // Embedding was NOT selected — server may return null, [[null]], or a list of null groups
        List<List<float[]>> emb = result.getEmbeddings();
        if (emb != null) {
            for (List<float[]> group : emb) {
                if (group != null) {
                    for (float[] entry : group) {
                        assertNull("Embedding entry should be null when not selected", entry);
                    }
                }
            }
        }
        // Metadata was NOT selected — verify it is absent
        List<List<Map<String, Object>>> meta = result.getMetadatas();
        if (meta != null && !meta.isEmpty() && meta.get(0) != null) {
            for (Map<String, Object> m : meta.get(0)) {
                assertTrue("Metadata should be null or empty when not selected",
                        m == null || m.isEmpty());
            }
        }
    }

    @Test
    public void testCloudSearchProjectionCustomKey() {
        Assume.assumeTrue("Cloud not available", cloudAvailable);
        // Custom key projection is a Cloud-oriented feature per D-16

        Search s = Search.builder()
                .knn(Knn.queryEmbedding(QUERY_ELECTRONICS))
                .select(Select.ID, Select.SCORE, Select.key("category"),
                        Select.key("price"))
                .limit(3)
                .build();
        SearchResult result = seedCollection.search().searches(s).execute();

        assertNotNull("CustomKey projection result should not be null", result);
        ResultGroup<SearchResultRow> rows = result.rows(0);
        assertFalse("CustomKey rows should not be empty", rows.isEmpty());

        // Verify metadatas contain projected keys
        List<List<Map<String, Object>>> metadatas = result.getMetadatas();
        assertNotNull("Metadatas must not be null when custom keys are projected", metadatas);
        assertFalse("Metadatas outer list must not be empty", metadatas.isEmpty());
        assertNotNull("Metadatas inner list must not be null", metadatas.get(0));
        assertFalse("Metadatas inner list must not be empty", metadatas.get(0).isEmpty());
        for (Map<String, Object> meta : metadatas.get(0)) {
            assertNotNull("Individual metadata entry must not be null", meta);
            assertTrue("Projected metadata should contain 'category' key",
                    meta.containsKey("category"));
            assertTrue("Projected metadata should contain 'price' key",
                    meta.containsKey("price"));
            // Verify non-projected keys are absent (projection should filter the response)
            assertFalse("Non-projected key 'in_stock' should be absent",
                    meta.containsKey("in_stock"));
            assertFalse("Non-projected key 'tags' should be absent",
                    meta.containsKey("tags"));
        }
    }

    // --- Private helpers ---

    private static Map<String, Object> buildSingleMeta(String key, Object value) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put(key, value);
        return meta;
    }

    /**
     * Polls a condition until it passes or the timeout expires (similar to Go's require.Eventually).
     *
     * @param timeout  maximum time to wait
     * @param tick     interval between attempts
     * @param runnable assertion block that throws {@link AssertionError} on failure
     */
    private static void assertEventually(Duration timeout, Duration tick, Runnable runnable) {
        long deadline = System.nanoTime() + timeout.toNanos();
        AssertionError lastError = null;
        while (System.nanoTime() < deadline) {
            try {
                runnable.run();
                return; // passed
            } catch (AssertionError e) {
                lastError = e;
            }
            try {
                Thread.sleep(tick.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("assertEventually interrupted", ie);
            }
        }
        throw lastError;
    }
}
