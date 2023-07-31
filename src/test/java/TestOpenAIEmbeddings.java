import org.junit.Test;
import tech.amikos.openai.CreateEmbeddingRequest;
import tech.amikos.openai.CreateEmbeddingResponse;
import tech.amikos.openai.OpenAIClient;

import java.util.Arrays;
import java.util.List;

public class TestOpenAIEmbeddings {


    @Test
    public void testSerialization() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input("Hello, my name is John. I am a Data Scientist."));
        System.out.println(req.json());
    }

    @Test
    public void testSerializationListOfStrings() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input(new String[]{"Hello, my name is John. I am a Data Scientist.", "Hello, my name is John. I am a Data Scientist."}));
        System.out.println(req.json());
    }

    @Test
    public void testSerializationListOfIntegers() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        req.input(new CreateEmbeddingRequest.Input(new Integer[]{1, 2, 3, 4, 5}));
        System.out.println(req.json());
    }

    @Test
    public void testSerializationListOfListOfIntegers() {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.model("text-ada");
        req.user("user-1234567890");
        List<Integer[]> list = Arrays.asList(new Integer[]{1, 2, 3}, new Integer[]{4, 5, 6});
        req.input(new CreateEmbeddingRequest.Input(list));
        System.out.println(req.json());
    }

    @Test
    public void testCreateEmbedding() {
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        CreateEmbeddingRequest req = new CreateEmbeddingRequest();
        req.input(new CreateEmbeddingRequest.Input("Hello, my name is John. I am a Data Scientist."));
        System.out.println(req.json());
        OpenAIClient client = new OpenAIClient();
        CreateEmbeddingResponse response = client.apiKey(apiKey)
                .createEmbedding(req);
        System.out.println(response);
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
        System.out.println(response);
    }
}
