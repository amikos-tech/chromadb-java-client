import org.junit.Test;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;
import tech.amikos.openai.CreateEmbeddingRequest;
import tech.amikos.openai.CreateEmbeddingResponse;
import tech.amikos.openai.OpenAIClient;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestOpenAIEmbeddings {


    @Test
    public void testSerialization() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input("Hello, my name is John. I am a Data Scientist."));
        assertEquals("{\"model\":\"text-ada\",\"user\":\"user-1234567890\",\"input\":\"Hello, my name is John. I am a Data Scientist.\"}", req.json());
    }

    @Test
    public void testSerializationListOfStrings() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input(new String[]{"Hello, my name is John. I am a Data Scientist.", "Hello, my name is John. I am a Data Scientist."}));
        assertEquals("{\"model\":\"text-ada\",\"user\":\"user-1234567890\",\"input\":[\"Hello, my name is John. I am a Data Scientist.\",\"Hello, my name is John. I am a Data Scientist.\"]}", req.json());
    }

    @Test
    public void testSerializationListOfIntegers() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input(new Integer[]{1, 2, 3, 4, 5}));
        assertEquals("{\"model\":\"text-ada\",\"user\":\"user-1234567890\",\"input\":[1,2,3,4,5]}", req.json());
    }

    @Test
    public void testSerializationListOfListOfIntegers() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        List<Integer[]> list = Arrays.asList(new Integer[]{1, 2, 3}, new Integer[]{4, 5, 6});
        req.input(new CreateEmbeddingRequest.Input(list));
        assertEquals("{\"model\":\"text-ada\",\"user\":\"user-1234567890\",\"input\":[[1,2,3],[4,5,6]]}", req.json());
    }

    @Test
    public void testCreateEmbedding() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.input(new CreateEmbeddingRequest.Input("Hello, my name is John. I am a Data Scientist."));
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(apiKey)
                .createEmbedding(req);
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals(1536, response.getData().get(0).getEmbedding().size());
    }

    @Test
    public void testCreateEmbeddingListOfStrings() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.input(new CreateEmbeddingRequest.Input(new String[]{"Hello, my name is John. I am a Data Scientist.", "Hello, my name is John. I am a Data Scientist."}));
        System.out.println(req.json());
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(apiKey)
                .createEmbedding(req);
        assertNotNull(response);
        assertEquals(2, response.getData().size());
        assertEquals(1536, response.getData().get(0).getEmbedding().size());
    }

    @Test
    public void testCreateEmbeddingsWithModels() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model("text-embedding-3-large");
        req.input(new CreateEmbeddingRequest.Input("Hello, my name is John. I am a Data Scientist."));
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(apiKey)
                .createEmbedding(req);
        assertEquals(3072, response.getData().get(0).getEmbedding().size());
    }

    @Test
    public void testEFBuilder() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<List<Float>> embeddings = OpenAIEmbeddingFunction.Instance()
                .withOpenAIAPIKey(apiKey)
                .withModelName("text-embedding-3-small")
                .build()
                .createEmbedding(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).size());
    }

    @Test
    public void testEFBuilderWithCustomURL() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<List<Float>> embeddings = OpenAIEmbeddingFunction.Instance()
                .withOpenAIAPIKey(apiKey)
                .withModelName("text-embedding-3-small")
                .withApiEndpoint("https://api.openai.com/v1/embeddings") // not really custom, but just to test the method
                .build()
                .createEmbedding(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).size());
    }


    @Test
    public void testCreateEmbeddingsWithModelsAndCustomURL() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model("text-embedding-3-small");

        OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small", "https://api.openai.com/v1/embeddings");

        List<List<Float>> embeddings = ef.createEmbedding(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).size());
    }
}
