package tech.amikos.chromadb.embeddings.cohere;

import org.junit.Test;
import tech.amikos.chromadb.Utils;

import static org.junit.Assert.*;

public class TestCohereEmbeddings {

    @Test
    public void testEmbeddings() {
        Utils.loadEnvFile(".env");
        CohereClient client = new CohereClient(Utils.getEnvOrProperty("COHERE_API_KEY"));
        String[] texts = {"Hello world", "How are you?"};
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(texts));
        assertEquals(2, response.getEmbeddings().size());
    }
}
