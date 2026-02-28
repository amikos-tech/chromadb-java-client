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
                .hnswBatchSize(500)
                .hnswSyncThreshold(250)
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
}
