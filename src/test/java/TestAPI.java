import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.handler.ApiException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class TestAPI {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8001);


    @Test
    public void testHeartbeat() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        Map<String, BigDecimal> hb = client.heartbeat();
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
        assertEquals(1, (int) collection.count());
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
        assertEquals(1, (int) collection.count());
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
        assertEquals(1, collection.get().getDocuments().size());
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
        assertEquals(1, (int) collection.count());
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
        collection.deleteWithIds(Arrays.asList("1"));
        assertEquals(0, collection.get().getDocuments().size());
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
        Map<String, String> where = new HashMap<>();
        where.put("key", "value");
        collection.deleteWhere(where);
        assertEquals(0, (int) collection.count());
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
        Map<String, String> where = new HashMap<>();
        where.put("key", "value2");
        collection.deleteWhere(where);
        assertEquals(1, (int) collection.count());
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
        Map<String, Object> whereDocuments = new HashMap<>();
        whereDocuments.put("$contains", "John");
        collection.deleteWhereDocuments(whereDocuments);
        assertEquals(0, (int) collection.count());

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
        Map<String, Object> whereDocuments = new HashMap<>();
        whereDocuments.put("$contains", "Mohn");
        collection.deleteWhereDocuments(whereDocuments);
        assertEquals(1, (int) collection.count());
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
        collection.update("test-collection2", null);
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
        collection.updateEmbeddings(null, null, Arrays.asList("Hello, my name is Bonh. I am a Data Scientist."), Arrays.asList("1"));

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
        assertEquals(1, (int) collection.count());
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
    }


    @Test
    public void testTimeoutOk() throws ApiException, IOException {
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}").withFixedDelay(2000)));

        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        client.setTimeout(3);
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
    }

    @Test(expected = ApiException.class)
    public void testTimeoutExpires() throws ApiException, IOException{
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}").withFixedDelay(2000)));

        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        client.setTimeout(1);
        try {
            client.heartbeat();
        } catch (ApiException e) {
            assertTrue(e.getMessage().contains("Read timed out") || e.getMessage().contains("timeout"));
            throw e;
        }

    }


    @Test
    public void testClientHeaders() throws ApiException, IOException {
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}")));
        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        client.setDefaultHeaders(new HashMap<String, String>() {{
            put("Your-Header-Key", "Your-Expected-Header-Value");
        }});
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
        // Verify that a GET request was made with a specific header
        verify(getRequestedFor(urlEqualTo("/api/v1/heartbeat"))
                .withHeader("Your-Header-Key", equalTo("Your-Expected-Header-Value")));
    }

    @Test
    public void testClientAuthorizationBasicHeader() throws ApiException, IOException {
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}")));
        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        String encodedString = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        client.setDefaultHeaders(new HashMap<String, String>() {{
            put("Authorization", "Basic "+encodedString);
        }});
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
        // Verify that a GET request was made with a specific header
        verify(getRequestedFor(urlEqualTo("/api/v1/heartbeat"))
                .withHeader("Authorization", equalTo("Basic "+encodedString)));
    }

    @Test
    public void testClientAuthorizationBearerHeader() throws ApiException, IOException {
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}")));
        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        client.setDefaultHeaders(new HashMap<String, String>() {{
            put("Authorization", "Bearer test-token");
        }});
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
        // Verify that a GET request was made with a specific header
        verify(getRequestedFor(urlEqualTo("/api/v1/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    public void testClientXChromaTokenHeader() throws ApiException, IOException {
        stubFor(get(urlEqualTo("/api/v1/heartbeat"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\": 123456789}")));
        Utils.loadEnvFile(".env");
        Client client = new Client("http://127.0.0.1:8001");
        client.setDefaultHeaders(new HashMap<String, String>() {{
            put("X-Chroma-Token", "test-token");
        }});
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
        // Verify that a GET request was made with a specific header
        verify(getRequestedFor(urlEqualTo("/api/v1/heartbeat"))
                .withHeader("X-Chroma-Token", equalTo("test-token")));
    }
}
