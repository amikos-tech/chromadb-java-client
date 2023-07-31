import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;

import java.io.IOException;
import java.util.*;

public class TestAPI {


    @Test
    public void testHeartbeat() throws ApiException, IOException {
        Client client = new Client("http://localhost:8000");
        System.out.println(client.heartbeat());
    }

    @Test
    public void testGetCollection() throws ApiException, IOException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        System.out.println(client.getCollection("test-collection", ef));
    }

    @Test
    public void testGetCollectionGet() throws ApiException, IOException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        System.out.println(client.getCollection("test-collection", ef).get());
    }


    @Test
    public void testCreateCollection() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection resp = client.createCollection("test-collection", null, true, ef);
        System.out.println(resp);
    }

    @Test
    public void testDeleteCollection() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        client.deleteCollection("test-collection");
    }

    @Test
    public void testCreateUpsert() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        Object resp = collection.upsert(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        System.out.println(resp);
        System.out.println(collection.get());
    }

    @Test
    public void testCreateAdd() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        Object resp = collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        System.out.println(resp);
        System.out.println(collection.get());
    }

    @Test
    public void testQuery() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        collection.add(null, metadata, Arrays.asList("Hello, my name is Bond. I am a Spy."), Arrays.asList("2"));
        LinkedTreeMap<String, Object> qr = collection.query(Arrays.asList("name is John"), 10, null, null, null);
        System.out.println(qr);
    }

    @Test
    public void testQueryExample() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Utils.loadEnvFile(".env");
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("type", "scientist");
        }});
        metadata.add(new HashMap<String, String>() {{
            put("type", "spy");
        }});
        collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
        LinkedTreeMap<String, Object> qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
        System.out.println(qr);
    }
}
