import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.handler.ApiException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class TestAPI {


    @Test
    public void testHeartbeat() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        Map<String, BigDecimal> hb = client.heartbeat();
        System.out.println(hb);
        assertTrue(hb.containsKey("nanosecond heartbeat"));
    }

    @Test
    public void testGetCollectionGet() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        System.out.println(client.getCollection("test-collection", ef).get());
        assertTrue(client.getCollection("test-collection", ef).get() != null);
    }


    @Test
    public void testCreateCollection() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        System.out.println(collection);
        assertEquals(collection.getName(), "test-collection");
    }

    @Test
    public void testDeleteCollection() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        client.deleteCollection("test-collection");

        try {
            client.getCollection("test-collection", ef);
        } catch (ApiException e) {
            assertEquals(e.getCode(), 500);
        }
    }

    @Test
    public void testCreateUpsert() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testCreateAdd() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testQuery() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        collection.add(null, metadata, Arrays.asList("Hello, my name is Bond. I am a Spy."), Arrays.asList("2"));
        Collection.QueryResponse qr = collection.query(Arrays.asList("name is John"), 10, null, null, null);
        System.out.println(qr);
        assertEquals(qr.getIds().get(0).get(0), "1"); //we check that Bond doc is first
    }

    @Test
    public void testQueryExample() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
        System.out.println(qr);
        assertEquals(qr.getIds().get(0).get(0), "2"); //we check that Bond doc is first
    }

    @Test
    public void testReset() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        client.reset();

        try {
            client.getCollection("test-collection", ef);
        } catch (ApiException e) {
            assertEquals(e.getCode(), 500);
        }
    }

    @Test
    public void testCreateAddCohere() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("COHERE_API_KEY");
        EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        Object resp = collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        System.out.println(resp);
        System.out.println(collection.get());
        assertTrue(collection.get().getDocuments().size() == 1);
    }

    @Test
    public void testQueryExampleCohere() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("COHERE_API_KEY");
        EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("type", "scientist");
        }});
        metadata.add(new HashMap<String, String>() {{
            put("type", "spy");
        }});
        List<String> texts = Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy.");
        collection.add(null, metadata, texts, Arrays.asList("1", "2"));
        Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
        assertEquals(qr.getIds().get(0).get(0), "2"); //we check that Bond doc is first
        System.out.println(qr);
    }

    @Test
    public void testListCollections() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        client.reset();
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        assertEquals(client.listCollections().size(), 1);
        System.out.println(client.listCollections());
    }

    @Test
    public void testCollectionCount() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        System.out.println(collection.count());
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testCollectionDeleteIds() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        System.out.println(collection.deleteWithIds(Arrays.asList("1")));
        System.out.println(collection.get());
        assertTrue(collection.get().getDocuments().size() == 0);
    }

    @Test
    public void testCollectionDeleteWhere() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        Map<String, String> where = new HashMap<>();
        where.put("key", "value");
        System.out.println(collection.deleteWhere(where));
        System.out.println(collection.get());
        assertTrue(collection.count() == 0);
    }

    @Test
    public void testCollectionDeleteWhereNoMatch() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        Map<String, String> where = new HashMap<>();
        where.put("key", "value2");
        System.out.println(collection.deleteWhere(where));
        System.out.println(collection.get());
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testCollectionDeleteWhereDocuments() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        Map<String, Object> whereDocuments = new HashMap<>();
        whereDocuments.put("$contains", "John");
        System.out.println(collection.deleteWhereDocuments(whereDocuments));
        System.out.println(collection.get());
        assertTrue(collection.count() == 0);

    }

    @Test
    public void testCollectionDeleteWhereDocumentsNoMatch() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        Map<String, Object> whereDocuments = new HashMap<>();
        whereDocuments.put("$contains", "Mohn");
        System.out.println(collection.deleteWhereDocuments(whereDocuments));
        System.out.println(collection.get());
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testCollectionCreateIndex() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        assumeTrue(client.version().equalsIgnoreCase("0.4.3"));
        client.reset();
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
        System.out.println(collection.createIndex());
    }

    @Test
    public void testVersion() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String version = client.version();
        System.out.println(version);
        assertNotNull(version);
    }


    @Test
    public void testUpdateCollection() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        collection.update("test-collection2", null);
        System.out.println(collection);
        assertEquals(collection.getName(), "test-collection2");
    }

    @Test
    public void testCollectionUpdateEmbeddings() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
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
        collection.updateEmbeddings(null, null, Arrays.asList("Hello, my name is Bonh. I am a Data Scientist."), Arrays.asList("1"));
        System.out.println(collection.get());

    }

    @Test
    public void testCreateAddHF() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("key", "value");
        }});
        Object resp = collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist."), Arrays.asList("1"));
        System.out.println(resp);
        System.out.println(collection.get());
        assertTrue(collection.count() == 1);
    }

    @Test
    public void testQueryExampleHF() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("HF_API_KEY");
        EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("type", "scientist");
        }});
        metadata.add(new HashMap<String, String>() {{
            put("type", "spy");
        }});
        List<String> texts = Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy.");
        collection.add(null, metadata, texts, Arrays.asList("1", "2"));
        Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
        assertEquals(qr.getIds().get(0).get(0), "2"); //we check that Bond doc is first
        System.out.println(qr);
    }


}
