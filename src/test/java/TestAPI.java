import org.junit.Test;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.*;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.ids.UUIDv4IdGenerator;
import tech.amikos.chromadb.model.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;

public class TestAPI {


    @Test
    public void testHeartbeat() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        Map<String, BigDecimal> hb = client.heartbeat();
        assertTrue(hb.containsKey("nanosecond heartbeat"));
    }

    @Test
    public void testCreateTenant() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        client.createTenant("test-tenant");
        Tenant tenant = client.getTenant("test-tenant");
        assertEquals("test-tenant", tenant.getName());
    }

    @Test
    public void testGetTenant() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        Tenant tenant = client.getTenant("default_tenant");
        assertEquals("default_tenant", tenant.getName());
    }

    @Test
    public void testCreateDatabaseWithDefaultActiveTenant() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        client.createDatabase("test-database");
        Database db = client.getDatabase("test-database");
        assertEquals("test-database", db.getName());
        assertEquals("default_tenant", db.getTenant());
        assertNotNull(db.getId());
    }

    @Test
    public void testCreateDatabaseOtherTenant() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String newTenant = "my-custom-tenant";
        client.createTenant(newTenant);
        client.createDatabase("test-database", newTenant);
        Database db = client.getDatabase("test-database", newTenant);
        assertEquals("test-database", db.getName());
        assertEquals(newTenant, db.getTenant());
        assertNotNull(db.getId());
    }

    @Test
    public void testGetCollectionGet() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        assertNotNull(client.getCollection("test-collection", ef).get());
    }

    @Test
    public void testGetCollectionGetWithWhere() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef).get(new GetEmbedding().where(
                WhereBuilder.create().eq("key", "value").build()
        ));
        assertNotNull(resp);
        assertEquals(1, resp.getIds().size());

    }

    @Test
    public void testGetCollectionGetWithWhereDocument() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef).get(new GetEmbedding().whereDocument(
                WhereDocumentBuilder.create().contains("John").build()
        ));
        assertNotNull(resp);
        assertEquals(1, resp.getIds().size());
        assertTrue(resp.getDocuments().get(0).contains("John"));
    }

    @Test
    public void testGetCollectionGetWithWhereAndWhereDocument() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        whereDocument(WhereDocumentBuilder.create().contains("John").build())
                        .where(WhereBuilder.create().eq("key", "value").build())
                );
        assertNotNull(resp);
        assertEquals(1, resp.getIds().size());
        assertTrue(resp.getDocuments().get(0).contains("John"));
    }

    @Test
    public void testGetCollectionWithInclude() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        include(Arrays.asList(GetEmbedding.IncludeEnum.DOCUMENTS, GetEmbedding.IncludeEnum.EMBEDDINGS))
                );
        assertNotNull(resp);
        assertEquals(2, resp.getIds().size());
        assertNotNull(resp.getDocuments());
        assertNotNull(resp.getEmbeddings());
        assertEquals(2, resp.getDocuments().size());
        assertEquals(2, resp.getEmbeddings().size());
    }

    @Test
    public void testGetCollectionWithIds() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        ids(Arrays.asList("1"))
                );
        assertNotNull(resp);
        assertEquals(1, resp.getIds().size());
        assertNotNull(resp.getDocuments());
        assertNull(resp.getEmbeddings());//by default embeddings are not included
        assertEquals(1, resp.getDocuments().size());
    }

    @Test
    public void testGetCollectionGetAll() throws ApiException, IOException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        collection.add(new AddEmbedding().ids(Arrays.asList("1", "2")).metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").build(),
                MetadataBuilder.create().forValue("key", "value2").build()
        )).documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
        Collection.GetResult resp = client.getCollection("test-collection", ef)
                .get(new GetEmbedding());
        assertNotNull(resp);
        assertEquals(2, resp.getIds().size());
        assertNotNull(resp.getDocuments());
        assertNull(resp.getEmbeddings());//by default embeddings are not included
        assertEquals(2, resp.getDocuments().size());
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
    public void testCreateCollectionWithMetadata() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", MetadataBuilder.create().forValue("test", "test").build(), true, ef);

        Collection afterCreate = client.getCollection("test-collection", ef);
        assertEquals(afterCreate.getName(), "test-collection");
        assertTrue("Metadata should contain key test", afterCreate.getMetadata().containsKey("test"));
        assertTrue(afterCreate.getMetadata().containsValue("test"));
    }

    @Test
    public void testCreateCollectionWithBuilder() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollectionWithBuilder("test-collection")
                .withCreateOrGet(true).withMetadata("test", "test")
                .withEmbeddingFunction(ef)
                .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
                .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
                .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
                .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
                .withIdGenerator(new UUIDv4IdGenerator())
                .withDocument("This is UUIDv4 id generated document.")
                .create();
        assertNotNull(collection);
        Collection afterCreate = client.getCollection("test-collection", ef);
        assertEquals(afterCreate.getName(), "test-collection");
        assertTrue("Metadata should contain key test", afterCreate.getMetadata().containsKey("test"));
        assertTrue("Metadata should contain hnsw:space", afterCreate.getMetadata().containsKey("hnsw:space"));
        assertEquals("Distance function should be cosine", afterCreate.getMetadata().get("hnsw:space"), HnswDistanceFunction.COSINE.getValue());
        assertTrue(afterCreate.getMetadata().containsValue("test"));
        Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        include(Arrays.asList(GetEmbedding.IncludeEnum.DOCUMENTS, GetEmbedding.IncludeEnum.EMBEDDINGS))
                );
        assertNotNull(resp);
        assertEquals(4, resp.getIds().size());
        assertNotNull(resp.getDocuments());
        assertNotNull(resp.getEmbeddings());
    }
    @Test
    public void testCollectionGetWithWhereOperationEq() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollectionWithBuilder("test-collection")
                .withCreateOrGet(true).withMetadata("test", "test")
                .withEmbeddingFunction(ef)
                .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
                .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
                .withDocument("Hello, my name is Bond. I am a Spy.","2")
                .create();
        assertNotNull(collection);
        Collection afterCreate = client.getCollection("test-collection", ef);
        afterCreate.get(new GetEmbedding().where(WhereBuilder.create().eq("id", "1")));
        assertEquals(afterCreate.getName(), "test-collection");
        assertTrue("Metadata should contain key test", afterCreate.getMetadata().containsKey("test"));
        assertTrue("Metadata should contain hnsw:space", afterCreate.getMetadata().containsKey("hnsw:space"));
        assertEquals("Distance function should be cosine", afterCreate.getMetadata().get("hnsw:space"), HnswDistanceFunction.COSINE.getValue());
        assertTrue(afterCreate.getMetadata().containsValue("test"));
        Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        include(Arrays.asList(GetEmbedding.IncludeEnum.DOCUMENTS, GetEmbedding.IncludeEnum.EMBEDDINGS))
                );
        assertNotNull(resp);
        assertEquals(4, resp.getIds().size());
        assertNotNull(resp.getDocuments());
        assertNotNull(resp.getEmbeddings());
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
    public void testQueryRequest() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        client.reset();
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollectionWithBuilder("test-collection")
                .withCreateOrGet(true).withMetadata("test", "test")
                .withEmbeddingFunction(ef)
                .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
                .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
                .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
                .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
                .withIdGenerator(new UUIDv4IdGenerator())
                .withDocument("This is UUIDv4 id generated document.")
                .create();
        Collection.QueryResponse qr = collection.query(new QueryEmbedding().queryTexts(Arrays.asList("who is named John?")).nResults(10));
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
    public void testCountCollections() throws ApiException {
        Utils.loadEnvFile(".env");
        Client client = new Client(Utils.getEnvOrProperty("CHROMA_URL"));
        String apiKey = Utils.getEnvOrProperty("OPENAI_API_KEY");
        client.reset();
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        client.createCollection("test-collection", null, true, ef);
        assertEquals(client.countCollections(), Integer.valueOf(1));
    }

}
