package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CollectionConfigurationTest {

    @Test
    public void testBuilderSetsAllFields() {
        CollectionConfiguration config = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(16)
                .hnswConstructionEf(100)
                .hnswSearchEf(50)
                .hnswBatchSize(1000)
                .hnswSyncThreshold(500)
                .build();

        assertEquals(DistanceFunction.COSINE, config.getSpace());
        assertEquals(Integer.valueOf(16), config.getHnswM());
        assertEquals(Integer.valueOf(100), config.getHnswConstructionEf());
        assertEquals(Integer.valueOf(50), config.getHnswSearchEf());
        assertEquals(Integer.valueOf(1000), config.getHnswBatchSize());
        assertEquals(Integer.valueOf(500), config.getHnswSyncThreshold());
    }

    @Test
    public void testBuilderDefaultsAreNull() {
        CollectionConfiguration config = CollectionConfiguration.builder().build();

        assertNull(config.getSpace());
        assertNull(config.getHnswM());
        assertNull(config.getHnswConstructionEf());
        assertNull(config.getHnswSearchEf());
        assertNull(config.getHnswBatchSize());
        assertNull(config.getHnswSyncThreshold());
    }

    @Test
    public void testDistanceFunctionValues() {
        assertEquals("cosine", DistanceFunction.COSINE.getValue());
        assertEquals("l2", DistanceFunction.L2.getValue());
        assertEquals("ip", DistanceFunction.IP.getValue());
    }

    @Test
    public void testIncludeValues() {
        assertEquals("embeddings", Include.EMBEDDINGS.getValue());
        assertEquals("documents", Include.DOCUMENTS.getValue());
        assertEquals("metadatas", Include.METADATAS.getValue());
        assertEquals("distances", Include.DISTANCES.getValue());
        assertEquals("uris", Include.URIS.getValue());
    }

    @Test
    public void testCreateCollectionOptionsWithMetadata() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("key", "value");

        CreateCollectionOptions options = CreateCollectionOptions.withMetadata(metadata);
        assertEquals("value", options.getMetadata().get("key"));
    }

    @Test
    public void testCreateCollectionOptionsDefensiveCopy() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("key", "value");

        CreateCollectionOptions options = CreateCollectionOptions.withMetadata(metadata);

        // Mutating the original map should not affect the options
        metadata.put("key", "changed");
        assertEquals("value", options.getMetadata().get("key"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateCollectionOptionsMetadataIsUnmodifiable() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("key", "value");

        CreateCollectionOptions options = CreateCollectionOptions.withMetadata(metadata);
        options.getMetadata().put("new-key", "new-value");
    }

    @Test
    public void testCreateCollectionOptionsBuilder() {
        CollectionConfiguration config = CollectionConfiguration.builder()
                .space(DistanceFunction.L2)
                .build();

        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .metadata(Collections.<String, Object>singletonMap("key", "value"))
                .configuration(config)
                .build();

        assertNotNull(options.getMetadata());
        assertNotNull(options.getConfiguration());
        assertEquals(DistanceFunction.L2, options.getConfiguration().getSpace());
    }

    @Test
    public void testCreateCollectionOptionsNullMetadata() {
        CreateCollectionOptions options = CreateCollectionOptions.builder()
                .configuration(CollectionConfiguration.builder().build())
                .build();

        assertNull(options.getMetadata());
    }
}
