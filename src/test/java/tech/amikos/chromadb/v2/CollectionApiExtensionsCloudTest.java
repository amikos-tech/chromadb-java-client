package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import tech.amikos.chromadb.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Cloud integration tests for fork, forkCount, and indexingStatus operations.
 *
 * <p>Credentials loaded from {@code .env} or environment variables:
 * CHROMA_API_KEY, CHROMA_TENANT, CHROMA_DATABASE.</p>
 *
 * <p>Fork tests are gated by {@code CHROMA_RUN_FORK_TESTS=true} (per D-16: fork costs ~$0.03/call).</p>
 *
 * <p>All tests are skipped when CHROMA_API_KEY is not set.</p>
 */
public class CollectionApiExtensionsCloudTest {

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

        Assume.assumeTrue("CHROMA_API_KEY is required for cloud extension tests", isNonBlank(apiKey));
        Assume.assumeTrue("CHROMA_TENANT is required for cloud extension tests", isNonBlank(tenant));
        Assume.assumeTrue("CHROMA_DATABASE is required for cloud extension tests", isNonBlank(database));

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
    public void testCloudForkCreatesCollection() {
        Assume.assumeTrue("Fork cloud test skipped — set CHROMA_RUN_FORK_TESTS=true to enable",
                "true".equals(System.getenv("CHROMA_RUN_FORK_TESTS")));
        String sourceName = uniqueCollectionName("cloud_fork_src_");
        trackCollection(sourceName);
        Collection source = client.createCollection(sourceName);
        String forkedName = uniqueCollectionName("cloud_fork_dst_");
        trackCollection(forkedName);
        Collection forked = source.fork(forkedName);
        assertNotNull(forked);
        assertEquals(forkedName, forked.getName());
        assertNotNull(forked.getId());
        assertFalse(forked.getId().isEmpty());
        // Forked collection is in same tenant/database
        assertEquals(source.getTenant(), forked.getTenant());
        assertEquals(source.getDatabase(), forked.getDatabase());
    }

    @Test
    public void testCloudForkCountReturnsZeroForNewCollection() {
        String name = uniqueCollectionName("cloud_forkcount_");
        trackCollection(name);
        Collection col = client.createCollection(name);
        try {
            int count = col.forkCount();
            assertEquals(0, count);
        } catch (ChromaNotFoundException e) {
            Assume.assumeTrue("forkCount endpoint not available on this Chroma Cloud account", false);
        }
    }

    @Test
    public void testCloudIndexingStatusReturnsValidFields() {
        String name = uniqueCollectionName("cloud_idxstatus_");
        trackCollection(name);
        Collection col = client.createCollection(name);
        IndexingStatus status = col.indexingStatus();
        assertNotNull(status);
        assertTrue("opIndexingProgress should be between 0.0 and 1.0",
                status.getOpIndexingProgress() >= 0.0 && status.getOpIndexingProgress() <= 1.0);
        assertTrue("totalOps should be >= 0", status.getTotalOps() >= 0);
        assertEquals("totalOps should equal numIndexedOps + numUnindexedOps",
                status.getTotalOps(), status.getNumIndexedOps() + status.getNumUnindexedOps());
    }

    // --- Helpers ---

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

    private static String uniqueCollectionName(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
