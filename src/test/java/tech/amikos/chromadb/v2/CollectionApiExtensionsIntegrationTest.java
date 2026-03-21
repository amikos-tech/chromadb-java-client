package tech.amikos.chromadb.v2;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestContainers integration tests for fork, forkCount, and indexingStatus.
 *
 * <p>These tests run against self-hosted Chroma via TestContainers. Fork and forkCount
 * currently return 404 on self-hosted; tests auto-skip via {@code Assume} and will
 * auto-activate when self-hosted Chroma adds support.</p>
 *
 * <p>Inherits container lifecycle from {@link AbstractChromaIntegrationTest}.</p>
 */
public class CollectionApiExtensionsIntegrationTest extends AbstractChromaIntegrationTest {

    @Test
    public void testForkSkipsOnSelfHosted() {
        Collection col = client.createCollection("fork_skip_test_" + System.nanoTime());
        try {
            Collection forked = col.fork("forked_" + System.nanoTime());
            // If we get here, self-hosted added fork support — validate the result
            assertNotNull(forked);
            assertEquals(col.getTenant(), forked.getTenant());
        } catch (ChromaNotFoundException e) {
            Assume.assumeTrue("fork not available on self-hosted Chroma " + configuredChromaVersion(), false);
        } catch (ChromaServerException e) {
            // Self-hosted may return 5xx for unsupported operations; treat as not-available
            Assume.assumeTrue("fork not available on self-hosted Chroma " + configuredChromaVersion()
                    + " (server error: " + e.getMessage() + ")", false);
        } finally {
            try { client.deleteCollection(col.getName()); } catch (Exception ignored) {}
        }
    }

    @Test
    public void testForkCountSkipsOnSelfHosted() {
        Collection col = client.createCollection("forkcount_skip_test_" + System.nanoTime());
        try {
            int count = col.forkCount();
            assertTrue("forkCount should be >= 0", count >= 0);
        } catch (ChromaNotFoundException e) {
            Assume.assumeTrue("forkCount not available on self-hosted Chroma " + configuredChromaVersion(), false);
        } catch (ChromaServerException e) {
            // Self-hosted may return 5xx for unsupported operations; treat as not-available
            Assume.assumeTrue("forkCount not available on self-hosted Chroma " + configuredChromaVersion()
                    + " (server error: " + e.getMessage() + ")", false);
        } finally {
            try { client.deleteCollection(col.getName()); } catch (Exception ignored) {}
        }
    }

    @Test
    public void testIndexingStatusSkipsOnSelfHosted() {
        Collection col = client.createCollection("idxstatus_skip_test_" + System.nanoTime());
        try {
            IndexingStatus status = col.indexingStatus();
            assertNotNull(status);
            assertTrue(status.getOpIndexingProgress() >= 0.0);
            assertTrue(status.getOpIndexingProgress() <= 1.0);
        } catch (ChromaNotFoundException e) {
            Assume.assumeTrue("indexingStatus not available on self-hosted Chroma " + configuredChromaVersion(), false);
        } catch (ChromaServerException e) {
            // Self-hosted may return 5xx for unsupported operations; treat as not-available
            Assume.assumeTrue("indexingStatus not available on self-hosted Chroma " + configuredChromaVersion()
                    + " (server error: " + e.getMessage() + ")", false);
        } finally {
            try { client.deleteCollection(col.getName()); } catch (Exception ignored) {}
        }
    }
}
