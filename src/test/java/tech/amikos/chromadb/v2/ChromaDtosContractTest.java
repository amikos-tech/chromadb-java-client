package tech.amikos.chromadb.v2;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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
                .build();

        Map<String, Object> configMap = ChromaDtos.toConfigurationMap(original);
        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(configMap);

        assertEquals(original, parsed);
    }

    @Test
    public void testConfigurationRoundTripSpannFields() {
        CollectionConfiguration original = CollectionConfiguration.builder()
                .space(DistanceFunction.IP)
                .spannSearchNprobe(11)
                .spannEfSearch(22)
                .build();

        Map<String, Object> configMap = ChromaDtos.toConfigurationMap(original);
        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(configMap);

        assertEquals(original, parsed);
    }

    @Test
    public void testParseConfigurationRejectsMixedHnswAndSpannFields() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:M", Integer.valueOf(16));
        config.put("spann:search_nprobe", Integer.valueOf(8));
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("both HNSW and SPANN"));
        }
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

    @Test
    public void testParseConfigurationRejectsBatchSizeBelowTwo() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:batch_size", Integer.valueOf(1));
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:batch_size must be >= 2"));
        }
    }

    @Test
    public void testParseConfigurationRejectsSyncThresholdBelowTwo() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("hnsw:sync_threshold", Integer.valueOf(1));
        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:sync_threshold must be >= 2"));
        }
    }

    @Test
    public void testCreateCollectionRequestIncludesTopLevelSchema() {
        Schema schema = Schema.builder()
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .cmek(Cmek.gcpKms("projects/p/locations/l/keyRings/r/cryptoKeys/k"))
                .build();

        ChromaDtos.CreateCollectionRequest request = new ChromaDtos.CreateCollectionRequest(
                "schema-col",
                null,
                null,
                ChromaDtos.toSchemaMap(schema),
                false
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> json = new Gson().fromJson(new Gson().toJson(request), Map.class);
        assertTrue(json.containsKey("schema"));
        Map<?, ?> schemaMap = (Map<?, ?>) json.get("schema");
        assertTrue(schemaMap.containsKey("keys"));
        assertTrue(schemaMap.containsKey("cmek"));
    }

    @Test
    public void testConfigurationRoundTripWithEmbeddingFunctionAndSchema() {
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("openai")
                .config(Collections.<String, Object>singletonMap("api_key_env_var", "OPENAI_API_KEY"))
                .build();

        Schema schema = Schema.builder()
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .embeddingFunction(spec)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        CollectionConfiguration original = CollectionConfiguration.builder()
                .hnswM(16)
                .embeddingFunction(spec)
                .schema(schema)
                .build();

        Map<String, Object> configMap = ChromaDtos.toConfigurationMap(original);
        assertTrue(configMap.containsKey("hnsw:M"));
        assertTrue(configMap.containsKey("embedding_function"));
        assertTrue(configMap.containsKey("schema"));

        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(configMap);
        assertEquals(original, parsed);
        assertEquals(spec, parsed.getEmbeddingFunction());
        assertEquals(schema, parsed.getSchema());
    }

    @Test
    public void testParseConfigurationSupportsSchemaCompatibilityField() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        Map<String, Object> keys = new LinkedHashMap<String, Object>();
        Map<String, Object> embeddingValueType = new LinkedHashMap<String, Object>();
        Map<String, Object> floatList = new LinkedHashMap<String, Object>();
        Map<String, Object> vectorIndex = new LinkedHashMap<String, Object>();
        Map<String, Object> vectorConfig = new LinkedHashMap<String, Object>();
        vectorConfig.put("space", "l2");
        vectorIndex.put("enabled", Boolean.TRUE);
        vectorIndex.put("config", vectorConfig);
        floatList.put("vector_index", vectorIndex);
        embeddingValueType.put("float_list", floatList);
        keys.put(Schema.EMBEDDING_KEY, embeddingValueType);
        schema.put("keys", keys);
        config.put("schema", schema);

        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(config);
        assertNotNull(parsed);
        assertNotNull(parsed.getSchema());
        assertEquals(
                DistanceFunction.L2,
                parsed.getSchema()
                        .getKey(Schema.EMBEDDING_KEY)
                        .getFloatList()
                        .getVectorIndex()
                        .getConfig()
                        .getSpace()
        );
    }

    @Test
    public void testParseConfigurationWrapsSchemaRuntimeValidationErrors() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        Map<String, Object> keys = new LinkedHashMap<String, Object>();
        keys.put(null, Collections.<String, Object>emptyMap());
        schema.put("keys", keys);
        config.put("schema", schema);

        try {
            ChromaDtos.parseConfiguration(config);
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("schema is invalid"));
        }
    }

    @Test
    public void testParseSchemaIgnoresUnknownSparseEmbeddingFunctionShape() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseVector = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseVectorIndex = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseConfig = new LinkedHashMap<String, Object>();
        sparseConfig.put("embedding_function", Collections.<String, Object>singletonMap("type", "unknown"));
        sparseConfig.put("bm25", Boolean.FALSE);
        sparseVectorIndex.put("enabled", Boolean.FALSE);
        sparseVectorIndex.put("config", sparseConfig);
        sparseVector.put("sparse_vector_index", sparseVectorIndex);
        defaults.put("sparse_vector", sparseVector);
        schema.put("defaults", defaults);

        Schema parsed = ChromaDtos.parseSchema(schema);
        assertNotNull(parsed);
        assertNotNull(parsed.getDefaults());
        assertNotNull(parsed.getDefaults().getSparseVector());
        assertNotNull(parsed.getDefaults().getSparseVector().getSparseVectorIndex());
        assertNotNull(parsed.getDefaults().getSparseVector().getSparseVectorIndex().getConfig());
        assertNull(parsed.getDefaults().getSparseVector().getSparseVectorIndex().getConfig().getEmbeddingFunction());
    }

    @Test
    public void testParseSchemaRejectsMalformedKnownSparseEmbeddingFunctionShape() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseVector = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseVectorIndex = new LinkedHashMap<String, Object>();
        Map<String, Object> sparseConfig = new LinkedHashMap<String, Object>();
        sparseConfig.put("embedding_function", Collections.<String, Object>singletonMap("type", "known"));
        sparseVectorIndex.put("enabled", Boolean.FALSE);
        sparseVectorIndex.put("config", sparseConfig);
        sparseVector.put("sparse_vector_index", sparseVectorIndex);
        defaults.put("sparse_vector", sparseVector);
        schema.put("defaults", defaults);

        try {
            ChromaDtos.parseSchema(schema);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("embedding_function.name must be a string"));
        }
    }
}
