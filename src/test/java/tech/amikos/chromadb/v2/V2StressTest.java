package tech.amikos.chromadb.v2;

import org.junit.BeforeClass;
import org.junit.Test;
import tech.amikos.chromadb.v2.AuthProvider;
import tech.amikos.chromadb.v2.Collection;
import tech.amikos.chromadb.v2.ChromaClient;
import tech.amikos.chromadb.v2.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class V2StressTest {
    private static ChromaClient client;

    @BeforeClass
    public static void setup() {
        String chromaUrl = System.getenv("CHROMA_URL");
        if (chromaUrl == null) {
            chromaUrl = "http://localhost:8000";
        }

        client = ChromaClient.builder()
            .serverUrl(chromaUrl)
            .auth(AuthProvider.none())
            .connectTimeout(60)
            .readTimeout(60)
            .writeTimeout(60)
            .tenant("default_tenant")
            .database("default_database")
            .build();
    }

    @Test
    public void testLargeScale() throws Exception {
        String collectionName = "stress_test_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        // Add 10,000 records in batches of 100
        for (int batch = 0; batch < 100; batch++) {
            List<String> ids = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();
            List<Map<String, Object>> metadatas = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                int recordId = batch * 100 + i;
                ids.add("id_" + recordId);

                // Create random embedding
                List<Float> embedding = new ArrayList<>();
                Random rand = new Random(recordId);
                for (int j = 0; j < 384; j++) {
                    embedding.add(rand.nextFloat());
                }
                embeddings.add(embedding);

                metadatas.add(Map.of(
                    "batch", batch,
                    "index", i,
                    "category", "category_" + (recordId % 10)
                ));
            }

            collection.add()
                .ids(ids)
                .embeddings(embeddings)
                .metadatas(metadatas)
                .execute();

            if (batch % 10 == 0) {
                System.out.println("Added " + ((batch + 1) * 100) + " records");
            }
        }

        assertEquals(10000, collection.count());
        System.out.println("Successfully added 10,000 records");

        // Test queries
        Random rand = new Random();
        List<Float> queryEmbedding = IntStream.range(0, 384)
            .mapToObj(i -> rand.nextFloat())
            .collect(Collectors.toList());

        QueryResponse result = collection.query()
            .queryEmbeddings(Arrays.asList(queryEmbedding))
            .nResults(100)
            .include(Include.METADATAS, Include.DISTANCES)
            .execute();

        assertEquals(1, result.getIds().size());
        assertEquals(100, result.getIds().get(0).size());

        client.deleteCollection(collectionName);
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        String collectionName = "concurrent_test_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Submit 100 concurrent operations
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            futures.add(executor.submit(() -> {
                try {
                    String id = "concurrent_" + taskId;
                    List<Float> embedding = IntStream.range(0, 384)
                        .mapToObj(j -> (float) (taskId * 0.01))
                        .collect(Collectors.toList());

                    collection.add()
                        .ids(Arrays.asList(id))
                        .embeddings(Arrays.asList(embedding))
                        .execute();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }));
        }

        // Wait for all operations to complete
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(30, TimeUnit.SECONDS));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(100, collection.count());
        client.deleteCollection(collectionName);
    }

    @Test
    public void testMemoryEfficiency() throws Exception {
        String collectionName = "memory_test_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Add records in a memory-efficient way
        int totalRecords = 5000;
        int batchSize = 50;

        for (int batch = 0; batch < totalRecords / batchSize; batch++) {
            List<String> ids = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();

            for (int i = 0; i < batchSize; i++) {
                int recordId = batch * batchSize + i;
                ids.add("mem_" + recordId);

                // Create embedding
                List<Float> embedding = new ArrayList<>();
                for (int j = 0; j < 384; j++) {
                    embedding.add((float) Math.random());
                }
                embeddings.add(embedding);
            }

            collection.add()
                .ids(ids)
                .embeddings(embeddings)
                .execute();

            // Clear local references
            ids = null;
            embeddings = null;
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (finalMemory - initialMemory) / (1024 * 1024); // MB

        System.out.println("Memory used: " + memoryUsed + " MB for " + totalRecords + " records");
        assertTrue("Memory usage should be reasonable", memoryUsed < 500); // Less than 500MB

        assertEquals(totalRecords, collection.count());
        client.deleteCollection(collectionName);
    }

    @Test
    public void testQueryPerformance() throws Exception {
        String collectionName = "query_perf_" + UUID.randomUUID().toString().substring(0, 8);
        Collection collection = client.createCollection(collectionName);

        // Add test data
        List<String> ids = new ArrayList<>();
        List<List<Float>> embeddings = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            ids.add("perf_" + i);

            List<Float> embedding = new ArrayList<>();
            for (int j = 0; j < 384; j++) {
                embedding.add((float) Math.random());
            }
            embeddings.add(embedding);

            metadatas.add(Map.of(
                "type", i % 2 == 0 ? "even" : "odd",
                "value", i
            ));
        }

        collection.add()
            .ids(ids)
            .embeddings(embeddings)
            .metadatas(metadatas)
            .execute();

        // Measure query performance
        List<Float> queryEmbedding = IntStream.range(0, 384)
            .mapToObj(i -> (float) Math.random())
            .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            QueryResponse result = collection.query()
                .queryEmbeddings(Arrays.asList(queryEmbedding))
                .nResults(10)
                .where(Where.eq("type", "even"))
                .execute();
            assertNotNull(result);
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgQueryTime = duration / 100.0;

        System.out.println("Average query time: " + avgQueryTime + " ms");
        assertTrue("Queries should be fast", avgQueryTime < 100); // Less than 100ms average

        client.deleteCollection(collectionName);
    }
}