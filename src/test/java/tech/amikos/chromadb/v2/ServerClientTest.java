package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.utility.DockerImageName;
import tech.amikos.chromadb.v2.auth.AuthProvider;
import tech.amikos.chromadb.v2.client.Collection;
import tech.amikos.chromadb.v2.client.ServerClient;
import tech.amikos.chromadb.v2.model.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ServerClientTest {
    private static ChromaDBContainer chromaContainer;
    private ServerClient client;

    @BeforeClass
    public static void setupContainer() {
        if (chromaContainer == null || !chromaContainer.isRunning()) {
            String chromaVersion = System.getenv("CHROMA_VERSION");
            if (chromaVersion == null) {
                chromaVersion = "0.5.15";
            }
            chromaContainer = new ChromaDBContainer("chromadb/chroma:" + chromaVersion)
                .withEnv("ALLOW_RESET", "TRUE");
            chromaContainer.start();

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Before
    public void setup() {
        String endpoint = chromaContainer.getEndpoint();

        client = ServerClient.builder()
            .baseUrl(endpoint)
            .auth(AuthProvider.none())
            .connectTimeout(30)
            .readTimeout(30)
            .writeTimeout(30)
            .defaultTenant("default")
            .defaultDatabase("default")
            .build();
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
        assertEquals("\"nanosecond heartbeat\"", heartbeat);
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

        Collection collection = client.createCollection(collectionName, builder -> builder
            .metadata(Map.of("test", "true", "created_at", System.currentTimeMillis()))
        );

        assertNotNull(collection);
        assertEquals(collectionName, collection.getName());
        assertNotNull(collection.getId());
        assertEquals("default", collection.getTenant());
        assertEquals("default", collection.getDatabase());
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

        collection.add(builder -> builder
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
        );

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

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2"))
            .embeddings(embeddings)
        );

        assertEquals(2, collection.count());
    }

    @Test
    public void testQuery() {
        String collectionName = "test_query_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
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
        );

        QueryResponse result = collection.query(builder -> builder
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.15f, 0.25f, 0.35f)))
            .nResults(2)
            .include(Include.DOCUMENTS, Include.DISTANCES)
        );

        assertNotNull(result);
        assertNotNull(result.getIds());
        assertEquals(1, result.getIds().size());
        assertEquals(2, result.getIds().get(0).size());
    }

    @Test
    public void testQueryWithFilter() {
        String collectionName = "test_query_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
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
        );

        Where where = Where.eq("type", "mammal");

        QueryResponse result = collection.query(builder -> builder
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.5f, 0.5f, 0.5f)))
            .nResults(10)
            .where(where)
            .include(Include.METADATAS, Include.DOCUMENTS)
        );

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
        assertEquals(2, result.getIds().get(0).size());
    }

    @Test
    public void testGet() {
        String collectionName = "test_get_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2", "id3"))
            .documents(Arrays.asList(
                "First document",
                "Second document",
                "Third document"
            ))
        );

        GetResponse result = collection.get(builder -> builder
            .ids(Arrays.asList("id1", "id3"))
            .include(Include.DOCUMENTS)
        );

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

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2", "id3"))
            .metadatas(Arrays.asList(
                Map.of("category", "A"),
                Map.of("category", "B"),
                Map.of("category", "A")
            ))
        );

        Where where = Where.eq("category", "A");

        GetResponse result = collection.get(builder -> builder
            .where(where)
            .include(Include.METADATAS)
        );

        assertNotNull(result);
        assertEquals(2, result.getIds().size());
        assertTrue(result.getIds().contains("id1"));
        assertTrue(result.getIds().contains("id3"));
    }

    @Test
    public void testUpdate() {
        String collectionName = "test_update_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList(
                "Original document 1",
                "Original document 2"
            ))
        );

        collection.update(builder -> builder
            .ids(Arrays.asList("id1"))
            .metadatas(Arrays.asList(Map.of("updated", true, "version", 2)))
            .documents(Arrays.asList("Updated document 1"))
        );

        GetResponse getResult = collection.get(builder -> builder
            .ids(Arrays.asList("id1"))
            .include(Include.DOCUMENTS, Include.METADATAS)
        );

        assertEquals("Updated document 1", getResult.getDocuments().get(0));
        assertEquals(true, getResult.getMetadatas().get(0).get("updated"));
    }

    @Test
    public void testUpsert() {
        String collectionName = "test_upsert_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1"))
            .documents(Arrays.asList("Original doc"))
        );

        collection.upsert(builder -> builder
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList("Document 1", "Document 2"))
        );

        assertEquals(2, collection.count());

        GetResponse getResult = collection.get(builder -> builder
            .ids(Arrays.asList("id1", "id2"))
            .include(Include.DOCUMENTS)
        );

        assertEquals(2, getResult.getDocuments().size());
    }

    @Test
    public void testDelete() {
        String collectionName = "test_delete_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2", "id3", "id4"))
        );

        assertEquals(4, collection.count());

        collection.delete(builder -> builder
            .ids(Arrays.asList("id1", "id3"))
        );

        assertEquals(2, collection.count());

        GetResponse getResult = collection.get(builder -> builder
            .include(Include.METADATAS)
        );

        assertEquals(2, getResult.getIds().size());
        assertTrue(getResult.getIds().contains("id2"));
        assertTrue(getResult.getIds().contains("id4"));
    }

    @Test
    public void testDeleteWithFilter() {
        String collectionName = "test_delete_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2", "id3"))
            .metadatas(Arrays.asList(
                Map.of("delete", true),
                Map.of("delete", false),
                Map.of("delete", true)
            ))
        );

        Where where = Where.eq("delete", true);

        collection.delete(builder -> builder
            .where(where)
        );

        assertEquals(1, collection.count());

        GetResponse getResult = collection.get(builder -> builder
            .include(Include.METADATAS)
        );

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

        collection.add(builder -> builder
            .ids(ids)
            .documents(documents)
            .metadatas(metadataList)
        );

        assertEquals(batchSize, collection.count());

        QueryResponse queryResult = collection.query(builder -> builder
            .queryEmbeddings(Arrays.asList(Arrays.asList(0.5f, 0.5f, 0.5f)))
            .nResults(10)
            .include(Include.METADATAS, Include.DISTANCES)
        );

        assertNotNull(queryResult);
        assertEquals(1, queryResult.getIds().size());
        assertEquals(10, queryResult.getIds().get(0).size());

        Where where = Where.eq("batch", 5);

        GetResponse batchResult = collection.get(builder -> builder
            .where(where)
            .include(Include.METADATAS)
        );

        assertEquals(10, batchResult.getIds().size());
    }

    @Test
    public void testComplexWhereFilters() {
        String collectionName = "test_complex_filter_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        collection.add(builder -> builder
            .ids(Arrays.asList("id1", "id2", "id3", "id4"))
            .metadatas(Arrays.asList(
                Map.of("age", 25, "city", "New York"),
                Map.of("age", 30, "city", "San Francisco"),
                Map.of("age", 35, "city", "New York"),
                Map.of("age", 28, "city", "Boston")
            ))
        );

        Where ageFilter = Where.gte("age", 30);

        GetResponse result = collection.get(builder -> builder
            .where(ageFilter)
            .include(Include.METADATAS)
        );

        assertEquals(2, result.getIds().size());

        Where complexFilter = Where.and(
                Where.eq("city", "New York"),
                Where.gt("age", 30)
            );

        GetResponse complexResult = collection.get(builder -> builder
            .where(complexFilter)
            .include(Include.METADATAS)
        );

        assertEquals(1, complexResult.getIds().size());
        assertEquals("id3", complexResult.getIds().get(0));
    }

    @Test
    public void testFluentBuilders() {
        String collectionName = "test_fluent_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        // Test fluent add
        collection.add()
            .ids(Arrays.asList("id1", "id2"))
            .documents(Arrays.asList("Doc 1", "Doc 2"))
            .metadatas(Arrays.asList(
                Map.of("type", "A"),
                Map.of("type", "B")
            ))
            .execute();

        assertEquals(2, collection.count());

        // Test fluent query
        QueryResponse queryResult = collection.query()
            .nResults(1)
            .include(Include.DOCUMENTS, Include.DISTANCES)
            .execute();

        assertNotNull(queryResult);
        assertEquals(1, queryResult.getIds().get(0).size());

        // Test fluent get
        GetResponse getResult = collection.get()
            .where(Where.eq("type", "A"))
            .include(Include.METADATAS)
            .execute();

        assertEquals(1, getResult.getIds().size());

        // Test fluent update
        collection.update()
            .ids(Arrays.asList("id1"))
            .documents(Arrays.asList("Updated Doc 1"))
            .execute();

        // Test fluent delete
        collection.delete()
            .ids(Arrays.asList("id2"))
            .execute();

        assertEquals(1, collection.count());
    }
}