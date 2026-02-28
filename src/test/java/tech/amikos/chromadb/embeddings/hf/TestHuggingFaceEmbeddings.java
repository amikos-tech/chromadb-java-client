package tech.amikos.chromadb.embeddings.hf;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.time.Duration;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestHuggingFaceEmbeddings {
    static GenericContainer hfeiContainer;

    private static String getTEIImage() {
        String version = System.getenv("TEI_VERSION") != null ? System.getenv("TEI_VERSION") : "1.8.3";
        String image = System.getenv("TEI_IMAGE") != null ? System.getenv("TEI_IMAGE") : "ghcr.io/huggingface/text-embeddings-inference";
        return image + ":cpu-" + version;
    }

    @BeforeClass
    public static void setup() throws Exception {
        Utils.loadEnvFile(".env");

        try {
            hfeiContainer = new GenericContainer(getTEIImage())
                    .withCommand("--model-id", "sentence-transformers/all-MiniLM-L6-v2")
                    .withExposedPorts(80)
                    .waitingFor(Wait.forLogMessage(".*Ready.*", 1).withStartupTimeout(Duration.ofMinutes(5)));
            hfeiContainer.start();
            System.setProperty("HFEI_URL", "http://" + hfeiContainer.getHost() + ":" + hfeiContainer.getMappedPort(80));
        } catch (Exception e) {
            System.err.println("HFEI container failed to start");
            throw e;
        }
    }

    @Test
    public void testEmbedDocuments() throws EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));
        List<Embedding> results = ef.embedDocuments(Arrays.asList("Hello world", "How are you?"));
        assertEquals(2, results.size());
        assertEquals(384, results.get(0).getDimensions());
    }

    @Test
    public void testEmbedQuery() throws EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(384, results.getDimensions());
    }

    @Test
    public void testWithModel() throws EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("sentence-transformers/all-mpnet-base-v2"));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(768, results.getDimensions());
    }

    @Test
    public void testWithURL() throws EFException {
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(
                WithParam.baseAPI(System.getProperty("HFEI_URL")),
                new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(384, results.getDimensions());
    }
}
