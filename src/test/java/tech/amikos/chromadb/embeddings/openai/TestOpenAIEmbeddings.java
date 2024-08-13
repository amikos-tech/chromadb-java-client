package tech.amikos.chromadb.embeddings.openai;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.Utils;

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
    public void testCreateEmbedding() throws EFException {
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
    public void testCreateEmbeddingListOfStrings() throws EFException {
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
    public void testCreateEmbeddingsWithModels() throws EFException {
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
    public void testEFBuilder() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<Embedding> embeddings = OpenAIEmbeddingFunction.Instance()
                .withOpenAIAPIKey(apiKey)
                .withModelName("text-embedding-3-small")
                .build()
                .embedDocuments(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).getDimensions());
    }

    @Test
    public void testEFBuilderWithCustomURL() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<Embedding> embeddings = OpenAIEmbeddingFunction.Instance()
                .withOpenAIAPIKey(apiKey)
                .withModelName("text-embedding-3-small")
                .withApiEndpoint("https://api.openai.com/v1/embeddings") // not really custom, but just to test the method
                .build()
                .embedDocuments(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).getDimensions());
    }


    @Test
    public void testEmbedQuery() throws EFException {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model("text-embedding-3-small");

        OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small", "https://api.openai.com/v1/embeddings");

        Embedding embeddings = ef.embedQuery("Hello, my name is John. I am a Data Scientist.");
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.getDimensions());
    }
}
