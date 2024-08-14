package tech.amikos.chromadb.embeddings.hf;

import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.Utils;
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
    public void testEmbeddings() throws EFException {
        HuggingFaceClient client = new HuggingFaceClient(Utils.getEnvOrProperty("HF_API_KEY"));
        client.modelId("sentence-transformers/all-MiniLM-L6-v2");
        String[] texts = {"Hello world", "How are you?"};
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(texts));
        assertEquals(2, response.getEmbeddings().size());
    }

    @Test
    public void testEmbedDocuments() throws ApiException, EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        List<Embedding> results = ef.embedDocuments(Arrays.asList("Hello world", "How are you?"));
        assertEquals(2, results.size());
        assertEquals(384, results.get(0).getDimensions());
    }

    @Test
    public void testEmbedQuery() throws ApiException, EFException {
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(384, results.getDimensions());
    }
}

