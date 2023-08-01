import org.junit.Test;
import tech.amikos.cohere.CohereClient;
import tech.amikos.cohere.CreateEmbeddingRequest;
import tech.amikos.cohere.CreateEmbeddingResponse;

import static org.junit.Assert.*;

public class TestCohereClient {

    @Test
    public void testEmbeddings() {
        Utils.loadEnvFile(".env");
        CohereClient client = new CohereClient(Utils.getEnvOrProperty("COHERE_API_KEY"));
        String[] texts = {"Hello world", "How are you?"};
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(texts));
        assertEquals(2, response.getEmbeddings().size());
    }
}
