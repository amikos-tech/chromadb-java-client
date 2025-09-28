package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ChromaClientTest {
    private static GenericContainer<?> chromaContainer;
    private ChromaClient client;

    @BeforeClass
    public static void setupContainer() {
        if (chromaContainer == null || !chromaContainer.isRunning()) {
            String chromaVersion = System.getenv("CHROMA_VERSION");
            if (chromaVersion == null) {
                chromaVersion = "1.1.0";  // Use version that supports v2 API
            }

            chromaContainer = new GenericContainer<>(DockerImageName.parse("chromadb/chroma:" + chromaVersion))
                .withExposedPorts(8000)
                .withEnv("ALLOW_RESET", "TRUE")
                .withEnv("IS_PERSISTENT", "FALSE")  // Use ephemeral mode for tests
                .waitingFor(Wait.forHttp("/api/v2/heartbeat")
                    .forPort(8000)
                    .forStatusCode(200));

            chromaContainer.start();

            // Additional wait for ChromaDB to be fully ready
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Before
    public void setup() {
        String host = chromaContainer.getHost();
        Integer port = chromaContainer.getMappedPort(8000);
        String endpoint = "http://" + host + ":" + port;

        client = ChromaClient.builder()
            .serverUrl(endpoint)
            .auth(AuthProvider.none())
            .connectTimeout(30)
            .readTimeout(30)
            .writeTimeout(30)
            .tenant("default_tenant")
            .database("default_database")
            .build();

        // Ensure database exists for v2 API
        try {
            client.createDatabase("default_database");
        } catch (Exception e) {
            // Database might already exist, that's okay
        }
    }

    @After
    public void cleanup() {
        if (client != null) {
            try {
                List<Collection> collections = client.listCollections();
                for (Collection collection : collections) {
                    if (collection.getName().startsWith("test_")) {
                        client.deleteCollection(collection.getId().toString());
                    }
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    public void testHeartbeat() {
        String heartbeat = client.heartbeat();
        assertNotNull(heartbeat);
        // Heartbeat returns nanoseconds as a number (converted to string)
        // Should be a large number (timestamp in nanoseconds)
        assertTrue(heartbeat.length() > 0);
        try {
            Double.parseDouble(heartbeat);
        } catch (NumberFormatException e) {
            fail("Heartbeat should be a valid number");
        }
    }

    @Test
    public void testVersion() {
        String version = client.version();
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    @Test
    public void testCreateCollection() {
        String collectionName = "test_collection_" + UUID.randomUUID().toString().substring(0, 8);

        Collection collection = client.createCollection(collectionName,
            Map.of("test", "true", "created_at", System.currentTimeMillis()));

        assertNotNull(collection);
        assertEquals(collectionName, collection.getName());
        assertNotNull(collection.getId());
        assertEquals("default_tenant", collection.getTenant());
        assertEquals("default_database", collection.getDatabase());
    }

    @Test
    public void testGetOrCreateCollection() {
        String collectionName = "test_collection_" + UUID.randomUUID().toString().substring(0, 8);

        Collection collection1 = client.getOrCreateCollection(collectionName);
        assertNotNull(collection1);
        assertEquals(collectionName, collection1.getName());

        Collection collection2 = client.getOrCreateCollection(collectionName);
        assertNotNull(collection2);
        assertEquals(collection1.getId(), collection2.getId());
    }

    @Test
    public void testAddDocuments() {
        String collectionName = "test_add_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .documents(Arrays.asList(
                "This is document one",
                "This is document two",
                "This is document three"
            ))
            .metadatas(Arrays.asList(
                Map.of("source", "test", "page", 1),
                Map.of("source", "test", "page", 2),
                Map.of("source", "test", "page", 3)
            ))
            .execute();

        assertEquals(3, collection.count());
    }

    @Test
    public void testAddEmbeddings() {
        String collectionName = "test_embeddings_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        List<List<Float>> embeddings = Arrays.asList(
            Arrays.asList(0.1f, 0.2f, 0.3f),
            Arrays.asList(0.4f, 0.5f, 0.6f)
        );

        collection.add()
            .ids(Arrays.asList("id1", "id2"))
            .embeddings(embeddings)
            .execute();

        assertEquals(2, collection.count());
    }

    @Test
    public void testQuery() {
        String collectionName = "test_query_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .embeddings(Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f),
                Arrays.asList(0.4f, 0.5f, 0.6f),
                Arrays.asList(0.7f, 0.8f, 0.9f)
            ))
            .documents(Arrays.asList(
                "The weather is nice today",
                "I love programming in Java",
                "ChromaDB is a vector database"
            ))
            .execute();

        QueryResponse result = collection.query()
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.15f, 0.25f, 0.35f)))
            .nResults(2)
            .include(Include.DOCUMENTS, Include.DISTANCES)
            .execute();

        assertNotNull(result);
        assertNotNull(result.getIds());
        assertEquals(1, result.getIds().size());
        assertEquals(2, result.getIds().get(0).size());
    }

    @Test
    public void testQueryWithFilter() {
        String collectionName = "test_query_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .documents(Arrays.asList(
                "Document about cats",
                "Document about dogs",
                "Document about birds"
            ))
            .metadatas(Arrays.asList(
                Map.of("animal", "cat", "type", "mammal"),
                Map.of("animal", "dog", "type", "mammal"),
                Map.of("animal", "bird", "type", "avian")
            ))
            .execute();

        Where where = Where.eq("type", "mammal");

        QueryResponse result = collection.query()
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.5f, 0.5f, 0.5f)))
            .nResults(10)
            .where(where)
            .include(Include.METADATAS, Include.DOCUMENTS)
            .execute();

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
        assertEquals(2, result.getIds().get(0).size());
    }

    @Test
    public void testGet() {
        String collectionName = "test_get_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .documents(Arrays.asList(
                "First document",
                "Second document",
                "Third document"
            ))
            .execute();

        GetResponse result = collection.get()
            .ids(Arrays.asList("id1", "id3"))
            .include(Include.DOCUMENTS)
            .execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
        assertTrue(result.getIds().contains("id1"));
        assertTrue(result.getIds().contains("id3"));
        assertNotNull(result.getDocuments());
        assertEquals(2, result.getDocuments().size());
    }

    @Test
    public void testGetWithFilter() {
        String collectionName = "test_get_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .metadatas(Arrays.asList(
                Map.of("category", "A"),
                Map.of("category", "B"),
                Map.of("category", "A")
            ))
            .execute();

        Where where = Where.eq("category", "A");

        GetResponse result = collection.get()
            .where(where)
            .include(Include.METADATAS)
            .execute();

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
        assertTrue(result.getIds().contains("id1"));
        assertTrue(result.getIds().contains("id3"));
    }

    @Test
    public void testUpdate() {
        String collectionName = "test_update_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList(
                "Original document 1",
                "Original document 2"
            ))
            .execute();

        collection.update()
            .ids(Arrays.asList("id1"))
            .metadatas(Arrays.asList(Map.of("updated", true, "version", 2)))
            .documents(Arrays.asList("Updated document 1"))
            .execute();

        GetResponse getResult = collection.get()
            .ids(Arrays.asList("id1"))
            .include(Include.DOCUMENTS, Include.METADATAS)
            .execute();

        assertEquals("Updated document 1", getResult.getDocuments().get(0));
        assertEquals(true, getResult.getMetadatas().get(0).get("updated"));
    }

    @Test
    public void testUpsert() {
        String collectionName = "test_upsert_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1"))
            .documents(Arrays.asList("Original doc"))
            .execute();

        collection.upsert()
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList("Document 1", "Document 2"))
            .execute();

        assertEquals(2, collection.count());

        GetResponse getResult = collection.get()
            .ids(Arrays.asList("id1", "id2"))
            .include(Include.DOCUMENTS)
            .execute();

        assertEquals(2, getResult.getDocuments().size());
    }

    @Test
    public void testDelete() {
        String collectionName = "test_delete_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3", "id4"))
            .execute();

        assertEquals(4, collection.count());

        collection.delete()
            .ids(Arrays.asList("id1", "id3"))
            .execute();

        assertEquals(2, collection.count());

        GetResponse getResult = collection.get()
            .include(Include.METADATAS)
            .execute();

        assertEquals(2, getResult.getIds().size());
        assertTrue(getResult.getIds().contains("id2"));
        assertTrue(getResult.getIds().contains("id4"));
    }

    @Test
    public void testDeleteWithFilter() {
        String collectionName = "test_delete_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3"))
            .metadatas(Arrays.asList(
                Map.of("delete", true),
                Map.of("delete", false),
                Map.of("delete", true)
            ))
            .execute();

        Where where = Where.eq("delete", true);

        collection.delete()
            .where(where)
            .execute();

        assertEquals(1, collection.count());

        GetResponse getResult = collection.get()
            .include(Include.METADATAS)
            .execute();

        assertEquals(1, getResult.getIds().size());
        assertEquals("id2", getResult.getIds().get(0));
    }

    @Test
    public void testListCollections() {
        String prefix = "test_list_" + UUID.randomUUID().toString().substring(0, 8);

        for (int i = 0; i < 3; i++) {
            client.createCollection(prefix + "_" + i);
        }

        List<Collection> collections = client.listCollections();
        assertNotNull(collections);
        assertTrue(collections.size() >= 3);

        int count = 0;
        for (Collection col : collections) {
            if (col.getName().startsWith(prefix)) {
                count++;
            }
        }
        assertEquals(3, count);
    }

    @Test
    public void testCountCollections() {
        int initialCount = client.countCollections();

        String collectionName = "test_count_" + UUID.randomUUID().toString().substring(0, 8);
        client.createCollection(collectionName);

        int newCount = client.countCollections();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    public void testLargeDataset() {
        String collectionName = "test_large_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        int batchSize = 100;
        List<String> ids = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<Map<String, Object>> metadataList = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            ids.add("id_" + i);
            documents.add("This is document number " + i + " with some content about topic " + (i % 10));
            metadataList.add(Map.of("batch", i / 10, "topic", i % 10));
        }

        collection.add()
            .ids(ids)
            .documents(documents)
            .metadatas(metadataList)
            .execute();

        assertEquals(batchSize, collection.count());

        QueryResponse queryResult = collection.query()
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.5f, 0.5f, 0.5f)))
            .nResults(10)
            .include(Include.METADATAS, Include.DISTANCES)
            .execute();

        assertNotNull(queryResult);
        assertEquals(1, queryResult.getIds().size());
        assertEquals(10, queryResult.getIds().get(0).size());

        Where where = Where.eq("batch", 5);

        GetResponse batchResult = collection.get()
            .where(where)
            .include(Include.METADATAS)
            .execute();

        assertEquals(10, batchResult.getIds().size());
    }

    @Test
    public void testComplexWhereFilters() {
        String collectionName = "test_complex_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add()
            .ids(Arrays.asList("id1", "id2", "id3", "id4"))
            .metadatas(Arrays.asList(
                Map.of("age", 25, "city", "New York"),
                Map.of("age", 30, "city", "San Francisco"),
                Map.of("age", 35, "city", "New York"),
                Map.of("age", 28, "city", "Boston")
            ))
            .execute();

        Where ageFilter = Where.gte("age", 30);

        GetResponse result = collection.get()
            .where(ageFilter)
            .include(Include.METADATAS)
            .execute();

        assertEquals(2, result.getIds().size());

        Where complexFilter = Where.and(
                Where.eq("city", "New York"),
                Where.gt("age", 30)
            );

        GetResponse complexResult = collection.get()
            .where(complexFilter)
            .include(Include.METADATAS)
            .execute();

        assertEquals(1, complexResult.getIds().size());
        assertEquals("id3", complexResult.getIds().get(0));
    }

    @Test
    public void testFluentBuilders() {
        String collectionName = "test_fluent_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        // Test fluent add - single approach, no consumer pattern
        collection.add()
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList("Doc 1", "Doc 2"))
            .metadatas(Arrays.asList(
                Map.of("type", "A"),
                Map.of("type", "B")
            ))
            .execute();

        assertEquals(2, collection.count());

        // Test fluent query - single approach
        QueryResponse queryResult = collection.query()
            .nResults(1)
            .include(Include.DOCUMENTS, Include.DISTANCES)
            .execute();

        assertNotNull(queryResult);
        assertEquals(1, queryResult.getIds().get(0).size());

        // Test fluent get - single approach
        GetResponse getResult = collection.get()
            .where(Where.eq("type", "A"))
            .include(Include.METADATAS)
            .execute();

        assertEquals(1, getResult.getIds().size());

        // Test fluent update - single approach
        collection.update()
            .ids(Arrays.asList("id1"))
            .documents(Arrays.asList("Updated Doc 1"))
            .execute();

        // Test fluent delete - single approach
        collection.delete()
            .ids(Arrays.asList("id2"))
            .execute();

        assertEquals(1, collection.count());
    }

    @Test
    public void testStronglyTypedMetadata() {
        String collectionName = "test_metadata_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        // Create metadata using builder
        Metadata metadata1 = Metadata.builder()
            .putString("title", "Document 1")
            .putInt("version", 1)
            .putBoolean("published", true)
            .putList("tags", Arrays.asList("tech", "ai"))
            .build();

        Metadata metadata2 = Metadata.builder()
            .putString("title", "Document 2")
            .putInt("version", 2)
            .putBoolean("published", false)
            .putDouble("score", 95.5)
            .build();

        collection.add()
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList("Doc 1 content", "Doc 2 content"))
            .metadatas(Arrays.asList(metadata1.toMap(), metadata2.toMap()))
            .execute();

        GetResponse result = collection.get()
            .ids(Arrays.asList("id1", "id2"))
            .include(Include.METADATAS)
            .execute();

        // Verify metadata retrieval
        Map<String, Object> retrievedMeta1 = result.getMetadatas().get(0);
        assertEquals("Document 1", retrievedMeta1.get("title"));
        assertEquals(1, retrievedMeta1.get("version"));
        assertEquals(true, retrievedMeta1.get("published"));

        Map<String, Object> retrievedMeta2 = result.getMetadatas().get(1);
        assertEquals("Document 2", retrievedMeta2.get("title"));
        assertEquals(2, retrievedMeta2.get("version"));
        assertEquals(false, retrievedMeta2.get("published"));
        assertEquals(95.5, retrievedMeta2.get("score"));
    }
}