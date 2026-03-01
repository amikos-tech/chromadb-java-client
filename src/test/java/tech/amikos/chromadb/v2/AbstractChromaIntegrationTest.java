package tech.amikos.chromadb.v2;

import org.junit.Before;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractChromaIntegrationTest {

    private static final ChromaDBContainer CHROMA;

    static {
        String version = System.getenv("CHROMA_VERSION");
        String image = (version != null && !version.isEmpty())
                ? "chromadb/chroma:" + version
                : "chromadb/chroma:latest";
        CHROMA = new ChromaDBContainer(DockerImageName.parse(image))
                .withEnv("ALLOW_RESET", "TRUE");
        CHROMA.start();
    }

    protected Client client;
    protected String tenantName;
    protected String databaseName;

    @Before
    public void setUp() {
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

    protected static String endpoint() {
        return CHROMA.getEndpoint();
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
