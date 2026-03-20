package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractChromaIntegrationTest {

    private static final String DEFAULT_CHROMA_VERSION = "1.5.5";
    private static final ChromaDBContainer CHROMA;
    private static final RuntimeException CHROMA_STARTUP_FAILURE;

    static {
        ChromaDBContainer chroma = null;
        RuntimeException startupFailure = null;
        String envVersion = System.getenv("CHROMA_VERSION");
        String version = (envVersion != null && !envVersion.isEmpty())
                ? envVersion
                : DEFAULT_CHROMA_VERSION;
        String image = "chromadb/chroma:" + version;
        try {
            chroma = new ChromaDBContainer(DockerImageName.parse(image))
                    .withEnv("ALLOW_RESET", "TRUE");
            chroma.start();
        } catch (RuntimeException e) {
            startupFailure = e;
        }
        CHROMA = chroma;
        CHROMA_STARTUP_FAILURE = startupFailure;
    }

    protected Client client;
    protected String tenantName;
    protected String databaseName;

    @Before
    public void setUp() {
        if (CHROMA_STARTUP_FAILURE != null) {
            throw new AssertionError(
                "Chroma container failed to start for version "
                    + configuredChromaVersion()
                    + " -- failing test (not skipping)",
                CHROMA_STARTUP_FAILURE);
        }
        if (client != null) {
            client.close();
        }
        tenantName = uniqueName("it_tenant_");
        databaseName = uniqueName("it_db_");

        Client bootstrapClient = ChromaClient.builder()
                .baseUrl(CHROMA.getEndpoint())
                .build();
        try {
            bootstrapClient.createTenant(tenantName);
        } finally {
            bootstrapClient.close();
        }

        Client tenantClient = ChromaClient.builder()
                .baseUrl(CHROMA.getEndpoint())
                .tenant(tenantName)
                .build();
        try {
            tenantClient.createDatabase(databaseName);
        } finally {
            tenantClient.close();
        }

        client = ChromaClient.builder()
                .baseUrl(CHROMA.getEndpoint())
                .tenant(tenantName)
                .database(databaseName)
                .build();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    protected static String endpoint() {
        if (CHROMA == null) {
            throw new IllegalStateException(
                    "Chroma Testcontainer is not available"
                            + " (image tag: " + configuredChromaVersion() + ")",
                    CHROMA_STARTUP_FAILURE
            );
        }
        return CHROMA.getEndpoint();
    }

    protected static String configuredChromaVersion() {
        String envVersion = System.getenv("CHROMA_VERSION");
        return (envVersion != null && !envVersion.isEmpty())
                ? envVersion
                : DEFAULT_CHROMA_VERSION;
    }

    /**
     * Skips the current test if the configured Chroma version is below {@code minVersion}.
     * Uses JUnit 4 {@link Assume#assumeTrue} so the test is marked as "skipped", not "failed".
     *
     * @param minVersion minimum required version in dotted format (e.g. "1.3.0")
     */
    protected static void assumeMinVersion(String minVersion) {
        Assume.assumeTrue(
            "Skipping: requires Chroma >= " + minVersion
                + ", currently running " + configuredChromaVersion(),
            compareVersions(configuredChromaVersion(), minVersion) >= 0
        );
    }

    /**
     * Compares two dotted-version strings segment by segment.
     * Returns negative if v1 &lt; v2, zero if equal, positive if v1 &gt; v2.
     */
    static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    protected static float[] embedding(int dim) {
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = (i + 1) * 0.1f;
        }
        return v;
    }

    protected static List<float[]> embeddings(int count, int dim) {
        List<float[]> list = new ArrayList<float[]>();
        for (int n = 0; n < count; n++) {
            float[] v = new float[dim];
            for (int i = 0; i < dim; i++) {
                v[i] = (n + 1) * 0.1f + i * 0.01f;
            }
            list.add(v);
        }
        return list;
    }

    private static String uniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
