package tech.amikos.chromadb.embeddings.cohere;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.Utils;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.List;

import static org.junit.Assert.*;

public class TestCohereEmbeddings {


    @Test
    public void testEmbedDocuments() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("COHERE_API_KEY");
        CohereEmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey));
        List<Embedding> results = ef.embedDocuments(new String[]{"Hello world", "How are you?"});
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(4096, results.get(0).getDimensions());
    }

    @Test
    public void testEmbedQuery() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("COHERE_API_KEY");
        CohereEmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(4096, results.getDimensions());
    }

    @Test
    public void testWithModel() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("COHERE_API_KEY");
        CohereEmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-light-v3.0"));
        Embedding results = ef.embedQuery("How are you?");
        assertNotNull(results);
        assertEquals(384, results.getDimensions());
    }


}
