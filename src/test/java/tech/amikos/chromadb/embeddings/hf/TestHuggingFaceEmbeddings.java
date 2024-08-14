package tech.amikos.chromadb.embeddings.hf;

import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.Utils;
import tech.amikos.chromadb.handler.ApiException;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class TestHuggingFaceEmbeddings {

    @BeforeClass
    public static void setup() {
        Utils.loadEnvFile(".env");
    }

    @Test
    public void testEmbeddings() {
        HuggingFaceClient client = new HuggingFaceClient(Utils.getEnvOrProperty("HF_API_KEY"));
        client.modelId("sentence-transformers/all-MiniLM-L6-v2");
        String[] texts = {"Hello world", "How are you?"};
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(texts));
        assertEquals(2, response.getEmbeddings().size());
    }

    @Test
    public void testEmbed() throws ApiException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        List<List<Float>> results = ef.createEmbedding(Arrays.asList("Hello world", "How are you?"));
        assertEquals(2, results.size());
        assertEquals(384, results.get(0).size());
    }

    @Test
    public void testEmbedWithModel() throws ApiException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        List<List<Float>> results = ef.createEmbedding(Arrays.asList("Hello world", "How are you?"), "sentence-transformers/all-mpnet-base-v2");
        assertEquals(2, results.size());
        assertEquals(768, results.get(0).size());
    }
}

