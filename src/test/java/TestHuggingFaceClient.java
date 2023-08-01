import org.junit.Test;
import tech.amikos.hf.CreateEmbeddingRequest;
import tech.amikos.hf.CreateEmbeddingResponse;
import tech.amikos.hf.HuggingFaceClient;

import static org.junit.Assert.assertEquals;

public class TestHuggingFaceClient {


    @Test
    public void testEmbeddings() {
        Utils.loadEnvFile(".env");
        HuggingFaceClient client = new HuggingFaceClient(Utils.getEnvOrProperty("HF_API_KEY"));
        client.modelId("sentence-transformers/all-MiniLM-L6-v2");
        String[] texts = {"Hello world", "How are you?"};
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().inputs(texts));
        assertEquals(2, response.getEmbeddings().size());
    }
}

