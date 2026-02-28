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
    public void testDistanceFunctionFromValue() {
        assertEquals(DistanceFunction.COSINE, DistanceFunction.fromValue("cosine"));
        assertEquals(DistanceFunction.L2, DistanceFunction.fromValue("L2"));
        assertEquals(DistanceFunction.IP, DistanceFunction.fromValue(" ip "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistanceFunctionFromValueRejectsUnknown() {
        DistanceFunction.fromValue("unknown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDistanceFunctionFromValueRejectsNull() {
        DistanceFunction.fromValue(null);
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
    public void testIncludeFromValue() {
        assertEquals(Include.DOCUMENTS, Include.fromValue("documents"));
        assertEquals(Include.DISTANCES, Include.fromValue("DISTANCES"));
        assertEquals(Include.URIS, Include.fromValue(" uris "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncludeFromValueRejectsUnknown() {
        Include.fromValue("unsupported");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncludeFromValueRejectsNull() {
        Include.fromValue(null);
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

    @Test(expected = IllegalArgumentException.class)
    public void testHnswMRejectsNonPositive() {
        CollectionConfiguration.builder().hnswM(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswConstructionEfRejectsNonPositive() {
        CollectionConfiguration.builder().hnswConstructionEf(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswSearchEfRejectsNonPositive() {
        CollectionConfiguration.builder().hnswSearchEf(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswBatchSizeRejectsNonPositive() {
        CollectionConfiguration.builder().hnswBatchSize(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswSyncThresholdRejectsNonPositive() {
        CollectionConfiguration.builder().hnswSyncThreshold(0);
    }

    @Test
    public void testCollectionConfigurationEqualsHashCodeAndToString() {
        CollectionConfiguration a = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(16)
                .hnswConstructionEf(100)
                .hnswSearchEf(50)
                .hnswBatchSize(1000)
                .hnswSyncThreshold(500)
                .build();

        CollectionConfiguration b = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(16)
                .hnswConstructionEf(100)
                .hnswSearchEf(50)
                .hnswBatchSize(1000)
                .hnswSyncThreshold(500)
                .build();

        CollectionConfiguration c = CollectionConfiguration.builder()
                .space(DistanceFunction.L2)
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("hnswM=16"));
    }
}
