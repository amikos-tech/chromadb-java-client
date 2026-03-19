package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.Utils;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Optional live-cloud parity checks for request/response and core search/config flows.
 *
 * <p>Credentials are loaded from environment (or .env via {@link Utils#loadEnvFile(String)}):
 * <ul>
 *   <li>CHROMA_API_KEY</li>
 *   <li>CHROMA_TENANT</li>
 *   <li>CHROMA_DATABASE</li>
 * </ul>
 * Tests are skipped when any value is missing.</p>
 */
public class CloudParityIntegrationTest {

    private Client client;
    private String tenant;
    private String database;
    private final List<String> createdCollections = new ArrayList<String>();

    @Before
    public void setUp() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        tenant = Utils.getEnvOrProperty("CHROMA_TENANT");
        database = Utils.getEnvOrProperty("CHROMA_DATABASE");

        Assume.assumeTrue("CHROMA_API_KEY is required for cloud parity tests", isNonBlank(apiKey));
        Assume.assumeTrue("CHROMA_TENANT is required for cloud parity tests", isNonBlank(tenant));
        Assume.assumeTrue("CHROMA_DATABASE is required for cloud parity tests", isNonBlank(database));

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
                    // Best-effort cleanup for optional live-cloud tests.
                }
            }
            client.close();
            client = null;
        }
        createdCollections.clear();
    }

    @Test
    public void testCloudSearchParityWithEmbeddingsAndWhereFilter() {
        Collection col = createCollection("cloud_search_parity_");

        col.add()
                .ids("left", "right", "center")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.7f, 0.3f, 0.0f}
                )
                .documents("left doc", "right doc", "center doc")
                .metadatas(Arrays.<Map<String, Object>>asList(
                        metadata("side", "left"),
                        metadata("side", "right"),
                        metadata("side", "left")
                ))
                .execute();

        QueryResult nearest = col.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(2)
                .include(Include.DISTANCES)
                .execute();

        assertNotNull(nearest.getIds());
        assertEquals(1, nearest.getIds().size());
        assertEquals(2, nearest.getIds().get(0).size());
        assertEquals("left", nearest.getIds().get(0).get(0));
        assertNotNull(nearest.getDistances());
        assertEquals(1, nearest.getDistances().size());
        assertEquals(2, nearest.getDistances().get(0).size());
        assertTrue(nearest.getDistances().get(0).get(0) <= nearest.getDistances().get(0).get(1));

        QueryResult filtered = col.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(3)
                .where(Where.eq("side", "left"))
                .execute();

        assertNotNull(filtered.getIds());
        assertEquals(1, filtered.getIds().size());
        assertFalse(filtered.getIds().get(0).isEmpty());
        assertTrue(filtered.getIds().get(0).contains("left"));
        assertFalse(filtered.getIds().get(0).contains("right"));
    }

    @Test
    public void testCloudCollectionLifecycleCrudAndPagination() {
        Collection first = createCollection("cloud_lifecycle_a_");
        Collection second = createCollection("cloud_lifecycle_b_");
        Collection third = createCollection("cloud_lifecycle_c_");

        Collection fetched = client.getCollection(first.getName());
        assertNotNull(fetched);
        assertEquals(first.getId(), fetched.getId());
        assertEquals(tenant, fetched.getTenant().getName());
        assertEquals(database, fetched.getDatabase().getName());

        List<Collection> allCollections = client.listCollections();
        assertContainsCollection(allCollections, first.getName());
        assertContainsCollection(allCollections, second.getName());
        assertContainsCollection(allCollections, third.getName());

        List<Collection> firstPage = client.listCollections(2, 0);
        assertNotNull(firstPage);
        assertTrue(firstPage.size() <= 2);

        List<Collection> secondPage = client.listCollections(2, 2);
        assertNotNull(secondPage);
        assertTrue(secondPage.size() <= 2);

        client.deleteCollection(second.getName());
        untrackCollection(second.getName());

        try {
            client.getCollection(second.getName());
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException expected) {
            // expected
        }
    }

    @Test
    public void testCloudGetOrCreateReturnsExistingCollection() {
        String name = uniqueCollectionName("cloud_goc_");
        trackCollection(name);

        Collection first = client.getOrCreateCollection(
                name,
                CreateCollectionOptions.builder()
                        .metadata(metadata("scope", "cloud"))
                        .build()
        );
        Collection second = client.getOrCreateCollection(name);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
        assertEquals(name, second.getName());
        assertEquals(tenant, second.getTenant().getName());
        assertEquals(database, second.getDatabase().getName());
    }

    @Test
    public void testCloudRecordCrudParity() {
        Collection col = createCollection("cloud_records_");

        col.add()
                .ids("r1", "r2", "r3")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f},
                        new float[]{0.0f, 0.0f, 1.0f}
                )
                .documents("cats are curious", "dogs are loyal", "birds can fly")
                .metadatas(Arrays.<Map<String, Object>>asList(
                        metadata("kind", "cat"),
                        metadata("kind", "dog"),
                        metadata("kind", "bird")
                ))
                .execute();

        assertEquals(3, col.count());

        GetResult beforeUpdate = col.get()
                .ids("r1", "r2")
                .include(Include.DOCUMENTS, Include.METADATAS)
                .execute();
        assertEquals(2, beforeUpdate.getIds().size());
        assertNotNull(beforeUpdate.getDocuments());
        assertNotNull(beforeUpdate.getMetadatas());

        col.update()
                .ids("r1", "r2")
                .documents("cats are agile", "dogs guard homes")
                .execute();

        GetResult afterUpdate = col.get()
                .ids("r1", "r2")
                .include(Include.DOCUMENTS)
                .execute();
        assertEquals(2, afterUpdate.getIds().size());
        assertTrue(afterUpdate.getDocuments().contains("cats are agile"));
        assertTrue(afterUpdate.getDocuments().contains("dogs guard homes"));

        col.upsert()
                .ids("r2", "r4")
                .embeddings(new float[]{0.1f, 0.9f, 0.0f}, new float[]{0.9f, 0.1f, 0.0f})
                .documents("dogs are playful", "foxes are clever")
                .execute();

        assertEquals(4, col.count());

        col.delete().ids("r1", "r4").execute();
        assertEquals(2, col.count());
    }

    @Test
    public void testCloudGetAndQuerySupportInlineDocumentAndIdFilters() {
        Collection col = createCollection("cloud_filters_");

        col.add()
                .ids("cat-1", "cat-2", "dog-1")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.9f, 0.1f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f}
                )
                .documents("cats like naps", "cats chase lasers", "dogs chase balls")
                .metadatas(Arrays.<Map<String, Object>>asList(
                        metadata("species", "cat"),
                        metadata("species", "cat"),
                        metadata("species", "dog")
                ))
                .execute();

        GetResult onlyCatsByDoc = col.get()
                .where(Where.documentContains("cats"))
                .include(Include.DOCUMENTS)
                .execute();
        assertEquals(2, onlyCatsByDoc.getIds().size());
        assertTrue(onlyCatsByDoc.getDocuments().get(0).contains("cats"));
        assertTrue(onlyCatsByDoc.getDocuments().get(1).contains("cats"));

        GetResult selectedById = col.get()
                .where(Where.idIn("cat-1", "dog-1"))
                .execute();
        assertEquals(2, selectedById.getIds().size());
        assertTrue(selectedById.getIds().contains("cat-1"));
        assertTrue(selectedById.getIds().contains("dog-1"));

        QueryResult queryNoDogs = col.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(3)
                .where(Where.documentNotContains("dogs"))
                .execute();
        assertEquals(1, queryNoDogs.getIds().size());
        assertFalse(queryNoDogs.getIds().get(0).isEmpty());
        assertFalse(queryNoDogs.getIds().get(0).contains("dog-1"));
    }

    @Test
    public void testCloudQueryTextsWithExplicitEmbeddingFunctionParity() {
        Collection created = createCollection("cloud_query_texts_");
        created.add()
                .ids("left", "right")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f}
                )
                .documents("left doc", "right doc")
                .execute();

        Collection fetched = client.getCollection(
                created.getName(),
                fixedEmbeddingFunction(new float[]{0.0f, 1.0f, 0.0f})
        );
        QueryResult result = fetched.query()
                .queryTexts("about the right side")
                .nResults(1)
                .execute();

        assertEquals(1, result.getIds().size());
        assertEquals(1, result.getIds().get(0).size());
        assertEquals("right", result.getIds().get(0).get(0));
    }

    @Test
    public void testCloudModifyNameAndMetadataRoundTrip() {
        Collection col = createCollection(
                "cloud_modify_roundtrip_",
                CreateCollectionOptions.builder()
                        .metadata(metadata("env", "cloud"))
                        .build()
        );

        col.modifyMetadata(metadata("owner", "qa"));
        Collection fetched = client.getCollection(col.getName());
        assertNotNull(fetched.getMetadata());
        assertEquals("qa", fetched.getMetadata().get("owner"));

        String oldName = col.getName();
        String renamed = uniqueCollectionName("cloud_renamed_");
        col.modifyName(renamed);
        renameTrackedCollection(oldName, renamed);

        Collection renamedCollection = client.getCollection(renamed);
        assertEquals(renamed, renamedCollection.getName());
        assertEquals(col.getId(), renamedCollection.getId());
    }

    @Test
    public void testCloudConfigurationParityWithRequestAuthoritativeFallback() {
        Collection col = createCollection("cloud_cfg_parity_");

        assertNotNull(col.getTenant());
        assertEquals(tenant, col.getTenant().getName());
        assertNotNull(col.getDatabase());
        assertEquals(database, col.getDatabase().getName());

        IndexGroup preferredGroup = detectIndexGroup(col);
        boolean usedHnsw = preferredGroup != IndexGroup.SPANN;
        Integer expectedHnswSearchEf = usedHnsw ? Integer.valueOf(123) : null;
        Integer expectedSpannNprobe = usedHnsw ? null : Integer.valueOf(16);
        try {
            if (usedHnsw) {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(expectedHnswSearchEf.intValue())
                        .build());
            } else {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(expectedSpannNprobe.intValue())
                        .build());
            }
        } catch (IllegalArgumentException e) {
            if (!isIndexGroupSwitchError(e)) {
                throw e;
            }
            // Fallback when server index-group shape was not explicit in initial payload.
            usedHnsw = !usedHnsw;
            expectedHnswSearchEf = usedHnsw ? Integer.valueOf(123) : null;
            expectedSpannNprobe = usedHnsw ? null : Integer.valueOf(16);
            if (usedHnsw) {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(expectedHnswSearchEf.intValue())
                        .build());
            } else {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(expectedSpannNprobe.intValue())
                        .build());
            }
        }

        assertNotNull(col.getConfiguration());
        if (usedHnsw) {
            assertEquals(expectedHnswSearchEf, col.getConfiguration().getHnswSearchEf());
        } else {
            assertEquals(expectedSpannNprobe, col.getConfiguration().getSpannSearchNprobe());
        }

        Collection fetched = client.getCollection(col.getName());
        assertNotNull(fetched.getTenant());
        assertEquals(tenant, fetched.getTenant().getName());
        assertNotNull(fetched.getDatabase());
        assertEquals(database, fetched.getDatabase().getName());

        if (fetched.getConfiguration() != null) {
            if (usedHnsw && fetched.getConfiguration().getHnswSearchEf() != null) {
                assertEquals(expectedHnswSearchEf, fetched.getConfiguration().getHnswSearchEf());
            }
            if (!usedHnsw && fetched.getConfiguration().getSpannSearchNprobe() != null) {
                assertEquals(expectedSpannNprobe, fetched.getConfiguration().getSpannSearchNprobe());
            }
        }

        fetched.add()
                .ids("cfg-left", "cfg-right")
                .embeddings(new float[]{1.0f, 0.0f, 0.0f}, new float[]{0.0f, 1.0f, 0.0f})
                .execute();

        QueryResult result = fetched.query()
                .queryEmbeddings(new float[]{1.0f, 0.0f, 0.0f})
                .nResults(1)
                .execute();

        assertEquals(1, result.getIds().size());
        assertEquals(1, result.getIds().get(0).size());
        assertEquals("cfg-left", result.getIds().get(0).get(0));
    }

    private Collection createCollection(String prefix) {
        return createCollection(prefix, null);
    }

    private Collection createCollection(String prefix, CreateCollectionOptions options) {
        String name = uniqueCollectionName(prefix);
        trackCollection(name);
        try {
            if (options == null) {
                return client.createCollection(name);
            }
            return client.createCollection(name, options);
        } catch (ChromaForbiddenException e) {
            throw new AssertionError(
                    "Cloud parity tests require collection write permissions (create/update/delete)"
                            + " for the configured CHROMA_TENANT/CHROMA_DATABASE."
                            + " Current credentials returned 403 Forbidden on createCollection.",
                    e
            );
        } catch (ChromaUnauthorizedException e) {
            throw new AssertionError(
                    "Cloud parity tests require a valid CHROMA_API_KEY."
                            + " Current credentials returned 401 Unauthorized.",
                    e
            );
        }
    }

    private static EmbeddingFunction fixedEmbeddingFunction(final float[] embedding) {
        return new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) throws EFException {
                return new Embedding(embedding);
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) throws EFException {
                List<Embedding> out = new ArrayList<Embedding>(documents.size());
                for (int i = 0; i < documents.size(); i++) {
                    out.add(new Embedding(embedding));
                }
                return out;
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) throws EFException {
                List<Embedding> out = new ArrayList<Embedding>(documents.length);
                for (int i = 0; i < documents.length; i++) {
                    out.add(new Embedding(embedding));
                }
                return out;
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

    private void trackCollection(String name) {
        createdCollections.add(name);
    }

    private void untrackCollection(String name) {
        for (int i = createdCollections.size() - 1; i >= 0; i--) {
            if (name.equals(createdCollections.get(i))) {
                createdCollections.remove(i);
            }
        }
    }

    private void renameTrackedCollection(String oldName, String newName) {
        untrackCollection(oldName);
        trackCollection(newName);
    }

    private static void assertContainsCollection(List<Collection> collections, String name) {
        for (Collection collection : collections) {
            if (name.equals(collection.getName())) {
                return;
            }
        }
        fail("Expected listCollections to include '" + name + "'");
    }

    private static Map<String, Object> metadata(String key, Object value) {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put(key, value);
        return metadata;
    }

    private static String uniqueCollectionName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isIndexGroupSwitchError(IllegalArgumentException e) {
        String message = e.getMessage();
        return message != null
                && message.contains("cannot switch collection index parameters between HNSW and SPANN");
    }

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

    private enum IndexGroup {
        HNSW,
        SPANN,
        UNKNOWN
    }
}
