package tech.amikos.chromadb.embeddings.hf;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.handler.ApiException;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestHuggingFaceEmbeddings {
    static GenericContainer hfeiContainer;

    @BeforeClass
    public static void setup() throws Exception {
        Utils.loadEnvFile(".env");

        try {
            hfeiContainer = new GenericContainer("ghcr.io/huggingface/text-embeddings-inference:cpu-1.5.0")
                    .withCommand("--model-id Snowflake/snowflake-arctic-embed-s --revision main")
                    .withExposedPorts(80)
                    .waitingFor(Wait.forHttp("/").forStatusCode(200));
            hfeiContainer.start();
            System.setProperty("HFEI_URL", "http://" + hfeiContainer.getHost() + ":" + hfeiContainer.getMappedPort(80));
        } catch (Exception e) {
            System.err.println("HFEI container failed to start");
            throw e;
        }
    }

    @Test
    public void testEmbedDocuments() throws ApiException, EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));
        List<Embedding> results = ef.embedDocuments(Arrays.asList("Hello world", "How are you?"));
        assertEquals(2, results.size());
        assertEquals(384, results.get(0).getDimensions());
    }

    @Test
    public void testEmbedQuery() throws ApiException, EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(384, results.getDimensions());
    }

    @Test
    public void testWithModel() throws ApiException, EFException {
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

