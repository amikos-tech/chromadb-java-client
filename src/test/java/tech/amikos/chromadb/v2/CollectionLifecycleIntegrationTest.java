package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CollectionLifecycleIntegrationTest extends AbstractChromaIntegrationTest {

    // --- create: plain ---

    @Test
    public void testCreateCollection() {
        Collection col = client.createCollection("test_col");
        assertNotNull(col);
        assertEquals("test_col", col.getName());
        assertNotNull(col.getId());
    }

    // --- create: with metadata ---

    @Test
    public void testCreateCollectionWithMetadata() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("env", "test");
        meta.put("version", 1.0);

        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .metadata(meta)
                .build();
        Collection col = client.createCollection("meta_col", options);

        assertNotNull(col.getMetadata());
        assertEquals("test", col.getMetadata().get("env"));
    }

    // --- create: with CollectionConfiguration ---

    @Test
    public void testCreateCollectionWithConfiguration() {
        CollectionConfiguration config = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(32)
                .hnswConstructionEf(200)
                .build();
        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .configuration(config)
                .build();

        Collection col = client.createCollection("config_col", options);
        assertNotNull(col);
        assertEquals("config_col", col.getName());
    }

    // --- create: duplicate → conflict ---

    @Test(expected = ChromaConflictException.class)
    public void testCreateDuplicateCollectionThrowsConflict() {
        client.createCollection("dup_col");
        client.createCollection("dup_col");
    }

    // --- get by name ---

    @Test
    public void testGetCollectionByName() {
        Collection created = client.createCollection("get_col");
        Collection fetched = client.getCollection("get_col");

        assertEquals(created.getId(), fetched.getId());
        assertEquals("get_col", fetched.getName());
    }

    // --- get nonexistent → not found ---

    @Test(expected = ChromaNotFoundException.class)
    public void testGetNonexistentCollectionThrowsNotFound() {
        client.getCollection("no_such_col");
    }

    // --- getOrCreate: create path ---

    @Test
    public void testGetOrCreateCreatesNewCollection() {
        Collection col = client.getOrCreateCollection("goc_col");
        assertNotNull(col);
        assertEquals("goc_col", col.getName());
    }

    // --- getOrCreate: get-existing path ---

    @Test
    public void testGetOrCreateReturnsExistingCollection() {
        Collection first = client.createCollection("goc_existing");
        Collection second = client.getOrCreateCollection("goc_existing");

        assertEquals(first.getId(), second.getId());
    }

    // --- getOrCreate: with options ---

    @Test
    public void testGetOrCreateWithOptions() {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("source", "test");
        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .metadata(meta)
                .build();

        Collection col = client.getOrCreateCollection("goc_opts", options);
        assertNotNull(col);
        assertEquals("goc_opts", col.getName());
    }

    // --- listCollections ---

    @Test
    public void testListCollections() {
        client.createCollection("list_a");
        client.createCollection("list_b");
        client.createCollection("list_c");

        List<Collection> cols = client.listCollections();
        assertEquals(3, cols.size());
    }

    // --- listCollections: with limit/offset pagination ---

    @Test
    public void testListCollectionsWithPagination() {
        client.createCollection("page_a");
        client.createCollection("page_b");
        client.createCollection("page_c");

        List<Collection> firstPage = client.listCollections(2, 0);
        assertEquals(2, firstPage.size());

        List<Collection> secondPage = client.listCollections(2, 2);
        assertEquals(1, secondPage.size());
    }

    // --- countCollections ---

    @Test
    public void testCountCollections() {
        assertEquals(0, client.countCollections());

        client.createCollection("count_a");
        client.createCollection("count_b");

        assertEquals(2, client.countCollections());
    }

    // --- delete collection ---

    @Test
    public void testDeleteCollection() {
        client.createCollection("del_col");
        assertEquals(1, client.countCollections());

        client.deleteCollection("del_col");
        assertEquals(0, client.countCollections());
    }

    // --- delete nonexistent → not found ---

    @Test(expected = ChromaNotFoundException.class)
    public void testDeleteNonexistentCollectionThrowsNotFound() {
        client.deleteCollection("no_such_col");
    }

    // --- modifyName ---

    @Test
    public void testModifyCollectionName() {
        Collection col = client.createCollection("old_name");
        col.modifyName("new_name");

        Collection fetched = client.getCollection("new_name");
        assertEquals(col.getId(), fetched.getId());
        assertEquals("new_name", fetched.getName());

        // old name should no longer exist
        try {
            client.getCollection("old_name");
            fail("Expected ChromaNotFoundException for old name");
        } catch (ChromaNotFoundException e) {
            // expected
        }
    }

    // --- modifyMetadata ---

    @Test
    public void testModifyCollectionMetadata() {
        Map<String, Object> initialMeta = new HashMap<String, Object>();
        initialMeta.put("key1", "value1");
        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .metadata(initialMeta)
                .build();
        Collection col = client.createCollection("meta_modify_col", options);

        Map<String, Object> newMeta = new HashMap<String, Object>();
        newMeta.put("key2", "value2");
        col.modifyMetadata(newMeta);

        Collection fetched = client.getCollection("meta_modify_col");
        assertNotNull(fetched.getMetadata());
        assertEquals("value2", fetched.getMetadata().get("key2"));
    }

    // --- modifyConfiguration ---

    @Test
    public void testModifyCollectionConfigurationHnswSearchEf() {
        CollectionConfiguration initialConfig = CollectionConfiguration.builder()
                .hnswSearchEf(64)
                .build();
        Collection col = client.createCollection(
                "config_modify_col",
                CreateCollectionOptions.builder().configuration(initialConfig).build()
        );

        col.modifyConfiguration(
                UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(200)
                        .build()
        );
        assertNotNull(col.getConfiguration());
        assertEquals(Integer.valueOf(200), col.getConfiguration().getHnswSearchEf());

        Collection fetched = client.getCollection("config_modify_col");
        assertNotNull(fetched.getConfiguration());
        assertEquals(Integer.valueOf(200), fetched.getConfiguration().getHnswSearchEf());
    }

    // --- count on empty collection ---

    @Test
    public void testCountOnEmptyCollection() {
        Collection col = client.createCollection("empty_col");
        assertEquals(0, col.count());
    }

}
