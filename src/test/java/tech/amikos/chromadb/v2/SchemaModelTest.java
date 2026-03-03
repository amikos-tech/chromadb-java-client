package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SchemaModelTest {

    @Test
    public void testCmekAcceptsValidGcpResource() {
        Cmek cmek = Cmek.gcpKms("projects/p/locations/l/keyRings/r/cryptoKeys/k");
        assertEquals(CmekProvider.GCP, cmek.getProvider());
        assertEquals("projects/p/locations/l/keyRings/r/cryptoKeys/k", cmek.getResource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCmekRejectsInvalidGcpResource() {
        Cmek.gcpKms("invalid-resource");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswBatchSizeValidation() {
        HnswIndexConfig.builder().batchSize(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHnswSyncThresholdValidation() {
        HnswIndexConfig.builder().syncThreshold(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpannSearchNprobeUpperBoundValidation() {
        SpannIndexConfig.builder().searchNprobe(129);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpannMergeThresholdLowerBoundValidation() {
        SpannIndexConfig.builder().mergeThreshold(24);
    }

    @Test
    public void testSpannQuantizationAliasParsing() {
        assertEquals(
                SpannQuantization.FOUR_BIT_RABIT_Q_WITH_U_SEARCH,
                SpannQuantization.fromValue("four_bit_rabbit_q_with_u_search")
        );
    }

    @Test
    public void testSpannQuantizationParsingUsesLocaleRoot() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(
                    SpannQuantization.FOUR_BIT_RABIT_Q_WITH_U_SEARCH,
                    SpannQuantization.fromValue("FOUR_BIT_RABIT_Q_WITH_U_SEARCH")
            );
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void testCmekProviderParsingUsesLocaleRoot() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(CmekProvider.GCP, CmekProvider.fromValue("GCP"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testVectorIndexConfigRejectsHnswAndSpannTogether() {
        VectorIndexConfig.builder()
                .hnsw(HnswIndexConfig.builder().efConstruction(100).build())
                .spann(SpannIndexConfig.builder().searchNprobe(64).build())
                .build();
    }

    @Test
    public void testSchemaRoundTripWithDefaultsKeysAndCmek() {
        EmbeddingFunctionSpec embeddingFunctionSpec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("openai")
                .config(Collections.<String, Object>singletonMap("api_key_env_var", "OPENAI_API_KEY"))
                .build();

        Schema schema = Schema.builder()
                .defaults(ValueTypes.builder()
                        .bool(BoolValueType.builder()
                                .boolInvertedIndex(BoolInvertedIndexType.builder()
                                        .enabled(false)
                                        .config(new BoolInvertedIndexConfig())
                                        .build())
                                .build())
                        .build())
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .enabled(true)
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .hnsw(HnswIndexConfig.builder()
                                                        .efConstruction(100)
                                                        .maxNeighbors(16)
                                                        .efSearch(10)
                                                        .batchSize(100)
                                                        .syncThreshold(1000)
                                                        .build())
                                                .embeddingFunction(embeddingFunctionSpec)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .cmek(Cmek.gcpKms("projects/p/locations/l/keyRings/r/cryptoKeys/k"))
                .build();

        Map<String, Object> schemaMap = ChromaDtos.toSchemaMap(schema);
        assertNotNull(schemaMap);
        assertTrue(schemaMap.containsKey("defaults"));
        assertTrue(schemaMap.containsKey("keys"));
        assertTrue(schemaMap.containsKey("cmek"));

        Schema parsed = ChromaDtos.parseSchema(schemaMap);
        assertEquals(schema, parsed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSchemaBuilderKeysRejectsBlankKey() {
        Map<String, ValueTypes> keys = new LinkedHashMap<String, ValueTypes>();
        keys.put(" ", ValueTypes.builder().build());
        Schema.builder().keys(keys);
    }

    @Test(expected = NullPointerException.class)
    public void testSchemaBuilderKeysRejectsNullValueType() {
        Map<String, ValueTypes> keys = new LinkedHashMap<String, ValueTypes>();
        keys.put("topic", null);
        Schema.builder().keys(keys);
    }

    @Test(expected = NullPointerException.class)
    public void testSchemaBuilderKeyRejectsNullValueType() {
        Schema.builder().key("topic", null);
    }

    @Test
    public void testEmbeddingFunctionSpecToStringRedactsApiKey() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("api_key", "secret-value");
        config.put("model_name", "text-embedding-3-small");

        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .name("openai")
                .config(config)
                .build();

        String rendered = spec.toString();
        assertFalse(rendered.contains("secret-value"));
        assertTrue(rendered.contains("***"));
    }

    @Test
    public void testEmbeddingFunctionSpecIsKnownTypeUsesLocaleRoot() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                    .type("KNOWN")
                    .name("openai")
                    .build();
            assertTrue(spec.isKnownType());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
