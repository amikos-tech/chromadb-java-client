package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import tech.amikos.chromadb.Utils;

import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Optional live-cloud integration checks for Chroma Cloud authentication.
 *
 * <p>Credentials are loaded from environment (or .env via {@link Utils#loadEnvFile(String)}):
 * <ul>
 *   <li>CHROMA_API_KEY</li>
 *   <li>CHROMA_TENANT</li>
 *   <li>CHROMA_DATABASE</li>
 * </ul>
 * Tests are skipped when any value is missing.</p>
 */
public class CloudAuthIntegrationTest {

    private Client client;

    @Before
    public void setUp() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("CHROMA_API_KEY");
        String tenant = Utils.getEnvOrProperty("CHROMA_TENANT");
        String database = Utils.getEnvOrProperty("CHROMA_DATABASE");

        Assume.assumeTrue("CHROMA_API_KEY is required for cloud integration tests", isNonBlank(apiKey));
        Assume.assumeTrue("CHROMA_TENANT is required for cloud integration tests", isNonBlank(tenant));
        Assume.assumeTrue("CHROMA_DATABASE is required for cloud integration tests", isNonBlank(database));

        client = ChromaClient.cloud()
                .apiKey(apiKey)
                .tenant(tenant)
                .database(database)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Test
    public void testCloudAuthAllowsPreFlightAndIdentity() {
        PreFlightInfo preFlightInfo = client.preFlight();
        assertNotNull(preFlightInfo);
        assertTrue(preFlightInfo.getMaxBatchSize() > 0);

        Identity identity = client.getIdentity();
        assertNotNull(identity);
        assertNotNull(identity.getUserId());
        assertFalse(identity.getUserId().trim().isEmpty());
        assertNotNull(identity.getTenant());
        assertFalse(identity.getTenant().trim().isEmpty());
        assertNotNull(identity.getDatabases());
    }

    @Test
    public void testCloudAuthAllowsCollectionListing() {
        assertNotNull(client.listCollections());
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
