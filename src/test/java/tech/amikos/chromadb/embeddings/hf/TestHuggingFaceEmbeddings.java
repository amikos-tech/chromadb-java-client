package tech.amikos.chromadb.embeddings.hf;

import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.handler.ApiException;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestHuggingFaceEmbeddings {

    @BeforeClass
    public static void setup() {
        Utils.loadEnvFile(".env");
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
}

