package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientLifecycleIntegrationTest extends AbstractChromaIntegrationTest {

    // --- heartbeat ---

    @Test
    public void testHeartbeatReturnsPositiveValue() {
        String heartbeat = client.heartbeat();
        assertNotNull(heartbeat);
        long value = Long.parseLong(heartbeat);
        assertTrue("heartbeat should be positive, got: " + value, value > 0);
    }

    // --- version ---

    @Test
    public void testVersionReturnsSemverString() {
        String version = client.version();
        assertNotNull(version);
        assertTrue("version should match semver pattern, got: " + version,
                version.matches("\\d+\\.\\d+\\.\\d+.*"));
    }

    // --- reset ---

    @Test
    public void testResetBehaviorDependsOnServerPolicy() {
        client.createCollection("col1");
        client.createCollection("col2");
        assertEquals(2, client.countCollections());

        try {
            client.reset();
            assertEquals(0, client.countCollections());
        } catch (ChromaForbiddenException e) {
            assertEquals(403, e.getStatusCode());
            assertNotNull(e.getMessage());
            assertFalse(e.getMessage().trim().isEmpty());
        }
    }

    // --- close and reopen ---

    @Test
    public void testCloseAndReopenClient() {
        client.close();

        Client newClient = ChromaClient.builder()
                .baseUrl(endpoint())
                .build();
        String heartbeat = newClient.heartbeat();
        assertNotNull(heartbeat);
        newClient.close();
    }

    // --- double close ---

    @Test
    public void testDoubleCloseIsHarmless() {
        client.close();
        client.close(); // should not throw
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }
}
