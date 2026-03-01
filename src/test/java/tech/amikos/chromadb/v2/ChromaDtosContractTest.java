package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChromaDtosContractTest {

    @Test
    public void testToFloatArrayRejectsNullElementWithIndex() {
        try {
            ChromaDtos.toFloatArray(Arrays.asList(1.0f, null, 3.0f));
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertEquals(200, e.getStatusCode());
            assertTrue(e.getMessage().contains("index 1"));
        }
    }

    @Test
    public void testConfigurationRoundTripAllFields() {
        CollectionConfiguration original = CollectionConfiguration.builder()
                .space(DistanceFunction.IP)
                .hnswM(32)
                .hnswConstructionEf(120)
                .hnswSearchEf(55)
                .hnswNumThreads(4)
                .hnswBatchSize(500)
                .hnswSyncThreshold(250)
                .hnswResizeFactor(1.25)
                .spannSearchNprobe(11)
                .spannEfSearch(22)
                .build();

        Map<String, Object> configMap = ChromaDtos.toConfigurationMap(original);
        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(configMap);

        assertEquals(original, parsed);
    }

    @Test
    public void testParseConfigurationRejectsNonStringSpace() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:space", Integer.valueOf(1));
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:space must be a string"));
        }
    }

    @Test
    public void testParseConfigurationRejectsUnknownSpace() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:space", "unknown");
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("unsupported hnsw:space value"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testParseConfigurationRejectsNonPositiveNumericValues() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:M", Integer.valueOf(0));
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:M must be > 0"));
        }
    }

    @Test
    public void testToUpdateConfigurationMapHnswWireShape() {
        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .hnswNumThreads(4)
                .hnswBatchSize(500)
                .hnswSyncThreshold(1000)
                .hnswResizeFactor(1.5)
                .build();

        Map<String, Object> map = ChromaDtos.toUpdateConfigurationMap(cfg);
        assertNotNull(map);
        assertTrue(map.containsKey("hnsw"));
        assertFalse(map.containsKey("spann"));

        Map<?, ?> hnsw = (Map<?, ?>) map.get("hnsw");
        assertEquals(Integer.valueOf(200), hnsw.get("ef_search"));
        assertEquals(Integer.valueOf(4), hnsw.get("num_threads"));
        assertEquals(Integer.valueOf(500), hnsw.get("batch_size"));
        assertEquals(Integer.valueOf(1000), hnsw.get("sync_threshold"));
        assertEquals(Double.valueOf(1.5), hnsw.get("resize_factor"));
    }

    @Test
    public void testToUpdateConfigurationMapOmitsUnsetHnswFields() {
        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .hnswSearchEf(200)
                .build();

        Map<String, Object> map = ChromaDtos.toUpdateConfigurationMap(cfg);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("hnsw"));

        Map<?, ?> hnsw = (Map<?, ?>) map.get("hnsw");
        assertEquals(1, hnsw.size());
        assertEquals(Integer.valueOf(200), hnsw.get("ef_search"));
    }

    @Test
    public void testToUpdateConfigurationMapSpannWireShape() {
        UpdateCollectionConfiguration cfg = UpdateCollectionConfiguration.builder()
                .spannSearchNprobe(32)
                .spannEfSearch(64)
                .build();

        Map<String, Object> map = ChromaDtos.toUpdateConfigurationMap(cfg);
        assertNotNull(map);
        assertTrue(map.containsKey("spann"));
        assertFalse(map.containsKey("hnsw"));

        Map<?, ?> spann = (Map<?, ?>) map.get("spann");
        assertEquals(Integer.valueOf(32), spann.get("search_nprobe"));
        assertEquals(Integer.valueOf(64), spann.get("ef_search"));
    }

    @Test
    public void testToUpdateConfigurationMapNullReturnsNull() {
        assertNull(ChromaDtos.toUpdateConfigurationMap(null));
    }

    @Test
    public void testParseConfigurationRejectsNonNumericNumThreads() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:num_threads", "x");
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:num_threads must be numeric"));
        }
    }

    @Test
    public void testParseConfigurationRejectsNonNumericResizeFactor() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:resize_factor", "x");
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:resize_factor must be numeric"));
        }
    }

    @Test
    public void testParseConfigurationRejectsNonNumericSpannSearchNprobe() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("spann:search_nprobe", "x");
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("spann:search_nprobe must be numeric"));
        }
    }

    @Test
    public void testParseConfigurationRejectsNonNumericSpannEfSearch() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("spann:ef_search", "x");
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("spann:ef_search must be numeric"));
        }
    }
}
