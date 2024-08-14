package tech.amikos.chromadb.embeddings.openai;

import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.Utils;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestOpenAIEmbeddings {
    @BeforeClass
    public static void setup() {
        Utils.loadEnvFile(".env");
    }

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
    public void testEmbedDocuments() throws EFException {
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<Embedding> embeddings = new OpenAIEmbeddingFunction(
                WithParam.apiKey(apiKey),
                WithParam.model("text-embedding-3-small"))
                .embedDocuments(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).getDimensions());
    }

    @Test
    public void testEFBuilderWithCustomURL() throws EFException {
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        List<Embedding> embeddings = new OpenAIEmbeddingFunction(
                WithParam.apiKey(apiKey),
                WithParam.model("text-embedding-3-small"),
                WithParam.baseAPI("https://api.openai.com/v1/embeddings"))
                .embedDocuments(Arrays.asList("Hello, my name is John. I am a Data Scientist."));
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.get(0).getDimensions());
    }


    @Test
    public void testEmbedQuery() throws EFException {
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model("text-embedding-3-small");
        OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey));
        Embedding embeddings = ef.embedQuery("Hello, my name is John. I am a Data Scientist.");
        assertNotNull(embeddings);
        assertEquals(1536, embeddings.getDimensions());
    }
}
