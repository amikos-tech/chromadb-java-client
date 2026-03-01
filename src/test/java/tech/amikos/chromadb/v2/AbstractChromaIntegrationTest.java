package tech.amikos.chromadb.v2;

import org.junit.Before;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

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

    @Before
    public void setUp() {
        if (client != null) {
            client.close();
        }
        client = ChromaClient.builder()
                .baseUrl(CHROMA.getEndpoint())
                .build();
        client.reset();
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
}
