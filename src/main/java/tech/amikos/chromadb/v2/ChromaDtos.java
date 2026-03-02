package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Package-private JSON request/response DTOs for Gson serialization.
 */
final class ChromaDtos {

    private static final Logger LOG = Logger.getLogger(ChromaDtos.class.getName());

    private ChromaDtos() {}

    // --- Requests ---

    static final class CreateTenantRequest {
        final String name;

        CreateTenantRequest(String name) {
            this.name = name;
        }
    }

    static final class CreateDatabaseRequest {
        final String name;

        CreateDatabaseRequest(String name) {
            this.name = name;
        }
    }

    static final class CreateCollectionRequest {
        final String name;
        final Map<String, Object> metadata;
        @SerializedName("configuration")
        final Map<String, Object> configuration;
        final Map<String, Object> schema;
        @SerializedName("get_or_create")
        final boolean getOrCreate;

        CreateCollectionRequest(String name, Map<String, Object> metadata,
                                Map<String, Object> configuration,
                                Map<String, Object> schema,
                                boolean getOrCreate) {
            this.name = name;
            this.metadata = metadata;
            this.configuration = configuration;
            this.schema = schema;
            this.getOrCreate = getOrCreate;
        }
    }

    static final class UpdateCollectionRequest {
        @SerializedName("new_name")
        final String newName;
        @SerializedName("new_metadata")
        final Map<String, Object> newMetadata;
        @SerializedName("new_configuration")
        final Map<String, Object> newConfiguration;

        UpdateCollectionRequest(String newName, Map<String, Object> newMetadata,
                                Map<String, Object> newConfiguration) {
            this.newName = newName;
            this.newMetadata = newMetadata;
            this.newConfiguration = newConfiguration;
        }
    }

    static final class AddRequest {
        final List<String> ids;
        final List<List<Float>> embeddings;
        final List<String> documents;
        final List<Map<String, Object>> metadatas;
        final List<String> uris;

        AddRequest(List<String> ids, List<List<Float>> embeddings,
                   List<String> documents, List<Map<String, Object>> metadatas,
                   List<String> uris) {
            this.ids = ids;
            this.embeddings = embeddings;
            this.documents = documents;
            this.metadatas = metadatas;
            this.uris = uris;
        }
    }

    static final class QueryRequest {
        @SerializedName("query_embeddings")
        final List<List<Float>> queryEmbeddings;
        @SerializedName("n_results")
        final int nResults;
        final Map<String, Object> where;
        @SerializedName("where_document")
        final Map<String, Object> whereDocument;
        final List<String> include;

        QueryRequest(List<List<Float>> queryEmbeddings, int nResults,
                     Map<String, Object> where, Map<String, Object> whereDocument,
                     List<String> include) {
            this.queryEmbeddings = queryEmbeddings;
            this.nResults = nResults;
            this.where = where;
            this.whereDocument = whereDocument;
            this.include = include;
        }
    }

    static final class GetRequest {
        final List<String> ids;
        final Map<String, Object> where;
        @SerializedName("where_document")
        final Map<String, Object> whereDocument;
        final List<String> include;
        final Integer limit;
        final Integer offset;

        GetRequest(List<String> ids, Map<String, Object> where,
                   Map<String, Object> whereDocument, List<String> include,
                   Integer limit, Integer offset) {
            this.ids = ids;
            this.where = where;
            this.whereDocument = whereDocument;
            this.include = include;
            this.limit = limit;
            this.offset = offset;
        }
    }

    static final class UpdateRequest {
        final List<String> ids;
        final List<List<Float>> embeddings;
        final List<String> documents;
        final List<Map<String, Object>> metadatas;

        UpdateRequest(List<String> ids, List<List<Float>> embeddings,
                      List<String> documents, List<Map<String, Object>> metadatas) {
            this.ids = ids;
            this.embeddings = embeddings;
            this.documents = documents;
            this.metadatas = metadatas;
        }
    }

    static final class UpsertRequest {
        final List<String> ids;
        final List<List<Float>> embeddings;
        final List<String> documents;
        final List<Map<String, Object>> metadatas;
        final List<String> uris;

        UpsertRequest(List<String> ids, List<List<Float>> embeddings,
                      List<String> documents, List<Map<String, Object>> metadatas,
                      List<String> uris) {
            this.ids = ids;
            this.embeddings = embeddings;
            this.documents = documents;
            this.metadatas = metadatas;
            this.uris = uris;
        }
    }

    static final class DeleteRequest {
        final List<String> ids;
        final Map<String, Object> where;
        @SerializedName("where_document")
        final Map<String, Object> whereDocument;

        DeleteRequest(List<String> ids, Map<String, Object> where,
                      Map<String, Object> whereDocument) {
            this.ids = ids;
            this.where = where;
            this.whereDocument = whereDocument;
        }
    }

    // --- Responses ---

    static final class TenantResponse {
        String name;
    }

    static final class DatabaseResponse {
        String name;
        String tenant;
    }

    static final class CollectionResponse {
        String id;
        String name;
        Map<String, Object> metadata;
        Integer dimension;
        @SerializedName("configuration_json")
        Map<String, Object> configurationJson;
        Map<String, Object> schema;
    }

    static final class QueryResponse {
        List<List<String>> ids;
        List<List<String>> documents;
        List<List<Map<String, Object>>> metadatas;
        List<List<List<Float>>> embeddings;
        List<List<Float>> distances;
        List<List<String>> uris;
    }

    static final class GetResponse {
        List<String> ids;
        List<String> documents;
        List<Map<String, Object>> metadatas;
        List<List<Float>> embeddings;
        List<String> uris;
    }

    static final class PreFlightResponse {
        @SerializedName("max_batch_size")
        Integer maxBatchSize;
        @SerializedName("supports_base64_encoding")
        Boolean supportsBase64Encoding;
    }

    static final class IdentityResponse {
        @SerializedName("user_id")
        String userId;
        String tenant;
        List<String> databases;
    }

    // --- Helpers ---

    static List<Float> toFloatList(float[] array) {
        if (array == null) {
            return null;
        }
        List<Float> list = new ArrayList<Float>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    static float[] toFloatArray(List<Float> list) {
        if (list == null) {
            return null;
        }
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Float value = list.get(i);
            if (value == null) {
                throw new ChromaDeserializationException(
                        "Server returned an embedding vector with a null value at index " + i,
                        200
                );
            }
            array[i] = value;
        }
        return array;
    }

    static List<List<Float>> toFloatLists(List<float[]> arrays) {
        if (arrays == null) {
            return null;
        }
        List<List<Float>> result = new ArrayList<List<Float>>(arrays.size());
        for (float[] array : arrays) {
            result.add(toFloatList(array));
        }
        return result;
    }

    static List<float[]> toFloatArrays(List<List<Float>> lists) {
        if (lists == null) {
            return null;
        }
        List<float[]> result = new ArrayList<float[]>(lists.size());
        for (List<Float> list : lists) {
            result.add(toFloatArray(list));
        }
        return result;
    }

    static Map<String, Object> toConfigurationMap(CollectionConfiguration config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        if (config.getSpace() != null) {
            params.put("hnsw:space", config.getSpace().getValue());
        }
        if (config.getHnswM() != null) {
            params.put("hnsw:M", config.getHnswM());
        }
        if (config.getHnswConstructionEf() != null) {
            params.put("hnsw:construction_ef", config.getHnswConstructionEf());
        }
        if (config.getHnswSearchEf() != null) {
            params.put("hnsw:search_ef", config.getHnswSearchEf());
        }
        if (config.getHnswNumThreads() != null) {
            params.put("hnsw:num_threads", config.getHnswNumThreads());
        }
        if (config.getHnswBatchSize() != null) {
            params.put("hnsw:batch_size", config.getHnswBatchSize());
        }
        if (config.getHnswSyncThreshold() != null) {
            params.put("hnsw:sync_threshold", config.getHnswSyncThreshold());
        }
        if (config.getHnswResizeFactor() != null) {
            params.put("hnsw:resize_factor", config.getHnswResizeFactor());
        }
        if (config.getSpannSearchNprobe() != null) {
            params.put("spann:search_nprobe", config.getSpannSearchNprobe());
        }
        if (config.getSpannEfSearch() != null) {
            params.put("spann:ef_search", config.getSpannEfSearch());
        }
        Map<String, Object> embeddingFunction = toEmbeddingFunctionSpecMap(config.getEmbeddingFunction());
        if (embeddingFunction != null) {
            params.put("embedding_function", embeddingFunction);
        }
        Map<String, Object> schema = toSchemaMap(config.getSchema());
        if (schema != null) {
            params.put("schema", schema);
        }
        if (params.isEmpty()) {
            return null;
        }
        return params;
    }

    static Map<String, Object> toSchemaMap(Schema schema) {
        if (schema == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Map<String, Object> defaults = toValueTypesMap(schema.getDefaults());
        if (defaults != null && !defaults.isEmpty()) {
            map.put("defaults", defaults);
        }
        if (schema.getKeys() != null && !schema.getKeys().isEmpty()) {
            Map<String, Object> keys = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, ValueTypes> entry : schema.getKeys().entrySet()) {
                Map<String, Object> valueTypes = toValueTypesMap(entry.getValue());
                if (valueTypes != null) {
                    keys.put(entry.getKey(), valueTypes);
                }
            }
            if (!keys.isEmpty()) {
                map.put("keys", keys);
            }
        }
        Map<String, Object> cmek = toCmekMap(schema.getCmek());
        if (cmek != null) {
            map.put("cmek", cmek);
        }
        if (map.isEmpty()) {
            return null;
        }
        return map;
    }

    static Map<String, Object> toUpdateConfigurationMap(UpdateCollectionConfiguration config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        if (config.hasHnswUpdates()) {
            Map<String, Object> hnsw = new LinkedHashMap<String, Object>();
            if (config.getHnswSearchEf() != null) {
                hnsw.put("ef_search", config.getHnswSearchEf());
            }
            if (config.getHnswNumThreads() != null) {
                hnsw.put("num_threads", config.getHnswNumThreads());
            }
            if (config.getHnswBatchSize() != null) {
                hnsw.put("batch_size", config.getHnswBatchSize());
            }
            if (config.getHnswSyncThreshold() != null) {
                hnsw.put("sync_threshold", config.getHnswSyncThreshold());
            }
            if (config.getHnswResizeFactor() != null) {
                hnsw.put("resize_factor", config.getHnswResizeFactor());
            }
            root.put("hnsw", hnsw);
        }
        if (config.hasSpannUpdates()) {
            Map<String, Object> spann = new LinkedHashMap<String, Object>();
            if (config.getSpannSearchNprobe() != null) {
                spann.put("search_nprobe", config.getSpannSearchNprobe());
            }
            if (config.getSpannEfSearch() != null) {
                spann.put("ef_search", config.getSpannEfSearch());
            }
            root.put("spann", spann);
        }
        if (root.isEmpty()) {
            throw new IllegalStateException(
                    "UpdateCollectionConfiguration has updates but serialized to an empty payload"
            );
        }
        return root;
    }

    static CollectionConfiguration parseConfiguration(Map<String, Object> configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return null;
        }
        CollectionConfiguration.Builder builder = CollectionConfiguration.builder();

        Object space = configJson.get("hnsw:space");
        if (space != null) {
            if (!(space instanceof String)) {
                throw invalidConfiguration("hnsw:space must be a string");
            }
            try {
                builder.space(DistanceFunction.fromValue((String) space));
            } catch (IllegalArgumentException e) {
                throw new ChromaDeserializationException(
                        "Server returned unsupported hnsw:space value: " + space,
                        200,
                        e
                );
            }
        }

        parsePositiveInt(configJson, "hnsw:M", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswM(value);
            }
        });
        parsePositiveInt(configJson, "hnsw:construction_ef", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswConstructionEf(value);
            }
        });
        parsePositiveInt(configJson, "hnsw:search_ef", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswSearchEf(value);
            }
        });
        parsePositiveInt(configJson, "hnsw:num_threads", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswNumThreads(value);
            }
        });
        parseAtLeastInt(configJson, "hnsw:batch_size", 2, builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswBatchSize(value);
            }
        });
        parseAtLeastInt(configJson, "hnsw:sync_threshold", 2, builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.hnswSyncThreshold(value);
            }
        });
        parsePositiveFiniteDouble(configJson, "hnsw:resize_factor", builder, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.hnswResizeFactor(value);
            }
        });
        parsePositiveInt(configJson, "spann:search_nprobe", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.spannSearchNprobe(value);
            }
        });
        parsePositiveInt(configJson, "spann:ef_search", builder, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.spannEfSearch(value);
            }
        });

        if (configJson.containsKey("embedding_function")) {
            Object rawSpec = configJson.get("embedding_function");
            try {
                builder.embeddingFunction(parseEmbeddingFunctionSpec(rawSpec, "embedding_function"));
            } catch (RuntimeException e) {
                throw invalidConfiguration("embedding_function is invalid: " + e.getMessage(), e);
            }
        }

        if (configJson.containsKey("schema")) {
            Object rawSchema = configJson.get("schema");
            if (rawSchema == null) {
                builder.schema(null);
            } else if (!(rawSchema instanceof Map)) {
                throw invalidConfiguration("schema must be an object");
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schemaMap = (Map<String, Object>) rawSchema;
                    builder.schema(parseSchema(schemaMap));
                } catch (RuntimeException e) {
                    throw invalidConfiguration("schema is invalid: " + e.getMessage(), e);
                }
            }
        }

        try {
            return builder.build();
        } catch (RuntimeException e) {
            throw invalidConfiguration(e.getMessage(), e);
        }
    }

    static Schema parseSchema(Map<String, Object> schemaJson) {
        if (schemaJson == null || schemaJson.isEmpty()) {
            return null;
        }

        Schema.Builder builder = Schema.builder();

        Object defaultsRaw = schemaJson.get("defaults");
        if (defaultsRaw != null) {
            builder.defaults(parseValueTypes(requireMap(defaultsRaw, "schema.defaults"), "schema.defaults"));
        }

        Object keysRaw = schemaJson.get("keys");
        if (keysRaw != null) {
            Map<String, Object> keysMap = requireMap(keysRaw, "schema.keys");
            for (Map.Entry<String, Object> entry : keysMap.entrySet()) {
                builder.key(
                        entry.getKey(),
                        parseValueTypes(requireMap(entry.getValue(), "schema.keys['" + entry.getKey() + "']"),
                                "schema.keys['" + entry.getKey() + "']")
                );
            }
        }

        Object cmekRaw = schemaJson.get("cmek");
        if (cmekRaw != null) {
            builder.cmek(parseCmek(requireMap(cmekRaw, "schema.cmek"), "schema.cmek"));
        }

        return builder.build();
    }

    private static ValueTypes parseValueTypes(Map<String, Object> valueTypesMap, String fieldName) {
        ValueTypes.Builder builder = ValueTypes.builder();

        Object stringRaw = valueTypesMap.get("string");
        if (stringRaw != null) {
            builder.string(parseStringValueType(requireMap(stringRaw, fieldName + ".string"), fieldName + ".string"));
        }

        Object floatListRaw = valueTypesMap.get("float_list");
        if (floatListRaw != null) {
            builder.floatList(parseFloatListValueType(requireMap(floatListRaw, fieldName + ".float_list"), fieldName + ".float_list"));
        }

        Object sparseVectorRaw = valueTypesMap.get("sparse_vector");
        if (sparseVectorRaw != null) {
            builder.sparseVector(parseSparseVectorValueType(
                    requireMap(sparseVectorRaw, fieldName + ".sparse_vector"), fieldName + ".sparse_vector"
            ));
        }

        Object intRaw = valueTypesMap.get("int");
        if (intRaw != null) {
            builder.integer(parseIntValueType(requireMap(intRaw, fieldName + ".int"), fieldName + ".int"));
        }

        Object floatRaw = valueTypesMap.get("float");
        if (floatRaw != null) {
            builder.floating(parseFloatValueType(requireMap(floatRaw, fieldName + ".float"), fieldName + ".float"));
        }

        Object boolRaw = valueTypesMap.get("bool");
        if (boolRaw != null) {
            builder.bool(parseBoolValueType(requireMap(boolRaw, fieldName + ".bool"), fieldName + ".bool"));
        }

        return builder.build();
    }

    private static StringValueType parseStringValueType(Map<String, Object> map, String fieldName) {
        StringValueType.Builder builder = StringValueType.builder();
        Object ftsRaw = map.get("fts_index");
        if (ftsRaw != null) {
            builder.ftsIndex(parseFtsIndexType(requireMap(ftsRaw, fieldName + ".fts_index"), fieldName + ".fts_index"));
        }
        Object invRaw = map.get("string_inverted_index");
        if (invRaw != null) {
            builder.stringInvertedIndex(parseStringInvertedIndexType(
                    requireMap(invRaw, fieldName + ".string_inverted_index"),
                    fieldName + ".string_inverted_index"
            ));
        }
        return builder.build();
    }

    private static FloatListValueType parseFloatListValueType(Map<String, Object> map, String fieldName) {
        FloatListValueType.Builder builder = FloatListValueType.builder();
        Object vectorRaw = map.get("vector_index");
        if (vectorRaw != null) {
            builder.vectorIndex(parseVectorIndexType(requireMap(vectorRaw, fieldName + ".vector_index"), fieldName + ".vector_index"));
        }
        return builder.build();
    }

    private static SparseVectorValueType parseSparseVectorValueType(Map<String, Object> map, String fieldName) {
        SparseVectorValueType.Builder builder = SparseVectorValueType.builder();
        Object sparseRaw = map.get("sparse_vector_index");
        if (sparseRaw != null) {
            builder.sparseVectorIndex(parseSparseVectorIndexType(
                    requireMap(sparseRaw, fieldName + ".sparse_vector_index"),
                    fieldName + ".sparse_vector_index"
            ));
        }
        return builder.build();
    }

    private static IntValueType parseIntValueType(Map<String, Object> map, String fieldName) {
        IntValueType.Builder builder = IntValueType.builder();
        Object raw = map.get("int_inverted_index");
        if (raw != null) {
            builder.intInvertedIndex(parseIntInvertedIndexType(
                    requireMap(raw, fieldName + ".int_inverted_index"),
                    fieldName + ".int_inverted_index"
            ));
        }
        return builder.build();
    }

    private static FloatValueType parseFloatValueType(Map<String, Object> map, String fieldName) {
        FloatValueType.Builder builder = FloatValueType.builder();
        Object raw = map.get("float_inverted_index");
        if (raw != null) {
            builder.floatInvertedIndex(parseFloatInvertedIndexType(
                    requireMap(raw, fieldName + ".float_inverted_index"),
                    fieldName + ".float_inverted_index"
            ));
        }
        return builder.build();
    }

    private static BoolValueType parseBoolValueType(Map<String, Object> map, String fieldName) {
        BoolValueType.Builder builder = BoolValueType.builder();
        Object raw = map.get("bool_inverted_index");
        if (raw != null) {
            builder.boolInvertedIndex(parseBoolInvertedIndexType(
                    requireMap(raw, fieldName + ".bool_inverted_index"),
                    fieldName + ".bool_inverted_index"
            ));
        }
        return builder.build();
    }

    private static VectorIndexType parseVectorIndexType(Map<String, Object> map, String fieldName) {
        VectorIndexType.Builder builder = VectorIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            builder.config(parseVectorIndexConfig(requireMap(config, fieldName + ".config"), fieldName + ".config"));
        }
        return builder.build();
    }

    private static FtsIndexType parseFtsIndexType(Map<String, Object> map, String fieldName) {
        FtsIndexType.Builder builder = FtsIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            requireMap(config, fieldName + ".config");
            builder.config(new FtsIndexConfig());
        }
        return builder.build();
    }

    private static SparseVectorIndexType parseSparseVectorIndexType(Map<String, Object> map, String fieldName) {
        SparseVectorIndexType.Builder builder = SparseVectorIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            builder.config(parseSparseVectorIndexConfig(requireMap(config, fieldName + ".config"), fieldName + ".config"));
        }
        return builder.build();
    }

    private static StringInvertedIndexType parseStringInvertedIndexType(Map<String, Object> map, String fieldName) {
        StringInvertedIndexType.Builder builder = StringInvertedIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            requireMap(config, fieldName + ".config");
            builder.config(new StringInvertedIndexConfig());
        }
        return builder.build();
    }

    private static IntInvertedIndexType parseIntInvertedIndexType(Map<String, Object> map, String fieldName) {
        IntInvertedIndexType.Builder builder = IntInvertedIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            requireMap(config, fieldName + ".config");
            builder.config(new IntInvertedIndexConfig());
        }
        return builder.build();
    }

    private static FloatInvertedIndexType parseFloatInvertedIndexType(Map<String, Object> map, String fieldName) {
        FloatInvertedIndexType.Builder builder = FloatInvertedIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            requireMap(config, fieldName + ".config");
            builder.config(new FloatInvertedIndexConfig());
        }
        return builder.build();
    }

    private static BoolInvertedIndexType parseBoolInvertedIndexType(Map<String, Object> map, String fieldName) {
        BoolInvertedIndexType.Builder builder = BoolInvertedIndexType.builder();
        Object enabled = map.get("enabled");
        if (enabled != null) {
            builder.enabled(requireBoolean(enabled, fieldName + ".enabled"));
        }
        Object config = map.get("config");
        if (config != null) {
            requireMap(config, fieldName + ".config");
            builder.config(new BoolInvertedIndexConfig());
        }
        return builder.build();
    }

    private static VectorIndexConfig parseVectorIndexConfig(Map<String, Object> map, String fieldName) {
        VectorIndexConfig.Builder builder = VectorIndexConfig.builder();
        Object space = map.get("space");
        if (space != null) {
            if (!(space instanceof String)) {
                throw new IllegalArgumentException(fieldName + ".space must be a string");
            }
            builder.space(DistanceFunction.fromValue((String) space));
        }

        Object sourceKey = map.get("source_key");
        if (sourceKey != null) {
            if (!(sourceKey instanceof String)) {
                throw new IllegalArgumentException(fieldName + ".source_key must be a string");
            }
            builder.sourceKey((String) sourceKey);
        }

        Object hnsw = map.get("hnsw");
        if (hnsw != null) {
            builder.hnsw(parseHnswIndexConfig(requireMap(hnsw, fieldName + ".hnsw"), fieldName + ".hnsw"));
        }

        Object spann = map.get("spann");
        if (spann != null) {
            builder.spann(parseSpannIndexConfig(requireMap(spann, fieldName + ".spann"), fieldName + ".spann"));
        }

        Object embeddingFunction = map.get("embedding_function");
        if (embeddingFunction != null) {
            builder.embeddingFunction(parseEmbeddingFunctionSpec(embeddingFunction, fieldName + ".embedding_function"));
        }

        return builder.build();
    }

    private static SparseVectorIndexConfig parseSparseVectorIndexConfig(Map<String, Object> map, String fieldName) {
        SparseVectorIndexConfig.Builder builder = SparseVectorIndexConfig.builder();
        Object sourceKey = map.get("source_key");
        if (sourceKey != null) {
            if (!(sourceKey instanceof String)) {
                throw new IllegalArgumentException(fieldName + ".source_key must be a string");
            }
            builder.sourceKey((String) sourceKey);
        }
        Object bm25 = map.get("bm25");
        if (bm25 != null) {
            builder.bm25(requireBoolean(bm25, fieldName + ".bm25"));
        }
        Object embeddingFunction = map.get("embedding_function");
        if (embeddingFunction != null) {
            builder.embeddingFunction(parseEmbeddingFunctionSpec(embeddingFunction, fieldName + ".embedding_function"));
        }
        return builder.build();
    }

    private static HnswIndexConfig parseHnswIndexConfig(Map<String, Object> map, String fieldName) {
        HnswIndexConfig.Builder builder = HnswIndexConfig.builder();
        parseInt(map, "ef_construction", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.efConstruction(value);
            }
        });
        parseInt(map, "max_neighbors", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.maxNeighbors(value);
            }
        });
        parseInt(map, "ef_search", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.efSearch(value);
            }
        });
        parseInt(map, "num_threads", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.numThreads(value);
            }
        });
        parseInt(map, "batch_size", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.batchSize(value);
            }
        });
        parseInt(map, "sync_threshold", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.syncThreshold(value);
            }
        });
        parseDouble(map, "resize_factor", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.resizeFactor(value);
            }
        });
        return builder.build();
    }

    private static SpannIndexConfig parseSpannIndexConfig(Map<String, Object> map, String fieldName) {
        SpannIndexConfig.Builder builder = SpannIndexConfig.builder();
        parseInt(map, "search_nprobe", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.searchNprobe(value);
            }
        });
        parseDouble(map, "search_rng_factor", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.searchRngFactor(value);
            }
        });
        parseDouble(map, "search_rng_epsilon", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.searchRngEpsilon(value);
            }
        });
        parseInt(map, "nreplica_count", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.nreplicaCount(value);
            }
        });
        parseDouble(map, "write_rng_factor", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.writeRngFactor(value);
            }
        });
        parseDouble(map, "write_rng_epsilon", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.writeRngEpsilon(value);
            }
        });
        parseInt(map, "split_threshold", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.splitThreshold(value);
            }
        });
        parseInt(map, "num_samples_kmeans", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.numSamplesKmeans(value);
            }
        });
        parseDouble(map, "initial_lambda", fieldName, new DoubleConsumer() {
            @Override
            public void accept(double value) {
                builder.initialLambda(value);
            }
        });
        parseInt(map, "reassign_neighbor_count", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.reassignNeighborCount(value);
            }
        });
        parseInt(map, "merge_threshold", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.mergeThreshold(value);
            }
        });
        parseInt(map, "num_centers_to_merge_to", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.numCentersToMergeTo(value);
            }
        });
        parseInt(map, "write_nprobe", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.writeNprobe(value);
            }
        });
        parseInt(map, "ef_construction", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.efConstruction(value);
            }
        });
        parseInt(map, "ef_search", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.efSearch(value);
            }
        });
        parseInt(map, "max_neighbors", fieldName, new IntConsumer() {
            @Override
            public void accept(int value) {
                builder.maxNeighbors(value);
            }
        });

        Object quantize = map.get("quantize");
        if (quantize != null) {
            if (!(quantize instanceof String)) {
                throw new IllegalArgumentException(fieldName + ".quantize must be a string");
            }
            builder.quantize(SpannQuantization.fromValue((String) quantize));
        }

        return builder.build();
    }

    private static EmbeddingFunctionSpec parseEmbeddingFunctionSpec(Object raw, String fieldName) {
        if (raw == null) {
            return null;
        }
        boolean lenientForUnknownSchemaSentinel = fieldName != null && fieldName.startsWith("schema.");
        Map<String, Object> map = requireMap(raw, fieldName);
        EmbeddingFunctionSpec.Builder builder = EmbeddingFunctionSpec.builder();
        Object rawType = map.get("type");
        Object rawName = map.get("name");
        boolean allowUnknownSchemaSentinel =
                lenientForUnknownSchemaSentinel && isUnknownSchemaDescriptor(rawType, rawName);

        if (rawType != null) {
            if (!(rawType instanceof String)) {
                if (allowUnknownSchemaSentinel) {
                    return ignoreUnknownSchemaEmbeddingFunction(
                            fieldName,
                            "type='unknown' schema sentinel with non-string type"
                    );
                }
                throw new IllegalArgumentException(fieldName + ".type must be a string");
            }
            builder.type((String) rawType);
        }

        if (!(rawName instanceof String)) {
            if (allowUnknownSchemaSentinel) {
                return ignoreUnknownSchemaEmbeddingFunction(
                        fieldName,
                        "type='unknown' schema sentinel without a valid name"
                );
            }
            throw new IllegalArgumentException(fieldName + ".name must be a string");
        }
        String normalizedName = ((String) rawName).trim();
        if (normalizedName.isEmpty()) {
            if (allowUnknownSchemaSentinel) {
                return ignoreUnknownSchemaEmbeddingFunction(
                        fieldName,
                        "type='unknown' schema sentinel with blank name"
                );
            }
            throw new IllegalArgumentException(fieldName + ".name must not be blank");
        }
        builder.name(normalizedName);

        Object config = map.get("config");
        if (config != null) {
            if (!(config instanceof Map)) {
                if (allowUnknownSchemaSentinel) {
                    return ignoreUnknownSchemaEmbeddingFunction(
                            fieldName,
                            "type='unknown' schema sentinel with non-object config"
                    );
                }
                throw new IllegalArgumentException(fieldName + ".config must be an object");
            }
            builder.config(requireMap(config, fieldName + ".config"));
        }

        try {
            return builder.build();
        } catch (RuntimeException e) {
            if (allowUnknownSchemaSentinel) {
                return ignoreUnknownSchemaEmbeddingFunction(
                        fieldName,
                        "type='unknown' schema sentinel failed validation: " + e.getMessage()
                );
            }
            throw e;
        }
    }

    private static EmbeddingFunctionSpec ignoreUnknownSchemaEmbeddingFunction(String fieldName, String reason) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(
                    Level.FINE,
                    "Ignoring schema embedding_function descriptor at {0}: {1}",
                    new Object[]{fieldName, reason}
            );
        }
        return null;
    }

    private static boolean isUnknownSchemaDescriptor(Object rawType, Object rawName) {
        if (!(rawType instanceof String)) {
            return false;
        }
        String normalizedType = ((String) rawType).trim().toLowerCase(Locale.ROOT);
        if (!"unknown".equals(normalizedType)) {
            return false;
        }
        if (rawName == null) {
            return true;
        }
        if (rawName instanceof String) {
            return ((String) rawName).trim().isEmpty();
        }
        return true;
    }

    private static Cmek parseCmek(Map<String, Object> map, String fieldName) {
        Object gcp = map.get("gcp");
        if (gcp != null) {
            if (!(gcp instanceof String)) {
                throw new IllegalArgumentException(fieldName + ".gcp must be a string");
            }
            return Cmek.gcpKms((String) gcp);
        }
        throw new IllegalArgumentException(fieldName + " must include a supported provider (gcp)");
    }

    private static Map<String, Object> toValueTypesMap(ValueTypes valueTypes) {
        if (valueTypes == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "string", toStringValueTypeMap(valueTypes.getString()));
        putIfNotNull(map, "float_list", toFloatListValueTypeMap(valueTypes.getFloatList()));
        putIfNotNull(map, "sparse_vector", toSparseVectorValueTypeMap(valueTypes.getSparseVector()));
        putIfNotNull(map, "int", toIntValueTypeMap(valueTypes.getInt()));
        putIfNotNull(map, "float", toFloatValueTypeMap(valueTypes.getFloat()));
        putIfNotNull(map, "bool", toBoolValueTypeMap(valueTypes.getBool()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toStringValueTypeMap(StringValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "fts_index", toFtsIndexTypeMap(valueType.getFtsIndex()));
        putIfNotNull(map, "string_inverted_index", toStringInvertedIndexTypeMap(valueType.getStringInvertedIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toFloatListValueTypeMap(FloatListValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "vector_index", toVectorIndexTypeMap(valueType.getVectorIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toSparseVectorValueTypeMap(SparseVectorValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "sparse_vector_index", toSparseVectorIndexTypeMap(valueType.getSparseVectorIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toIntValueTypeMap(IntValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "int_inverted_index", toIntInvertedIndexTypeMap(valueType.getIntInvertedIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toFloatValueTypeMap(FloatValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "float_inverted_index", toFloatInvertedIndexTypeMap(valueType.getFloatInvertedIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toBoolValueTypeMap(BoolValueType valueType) {
        if (valueType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "bool_inverted_index", toBoolInvertedIndexTypeMap(valueType.getBoolInvertedIndex()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toVectorIndexTypeMap(VectorIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toVectorIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toFtsIndexTypeMap(FtsIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toFtsIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toSparseVectorIndexTypeMap(SparseVectorIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toSparseVectorIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toStringInvertedIndexTypeMap(StringInvertedIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toStringInvertedIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toIntInvertedIndexTypeMap(IntInvertedIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toIntInvertedIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toFloatInvertedIndexTypeMap(FloatInvertedIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toFloatInvertedIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toBoolInvertedIndexTypeMap(BoolInvertedIndexType indexType) {
        if (indexType == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("enabled", Boolean.valueOf(indexType.isEnabled()));
        putIfNotNull(map, "config", toBoolInvertedIndexConfigMap(indexType.getConfig()));
        return map;
    }

    private static Map<String, Object> toVectorIndexConfigMap(VectorIndexConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (config.getSpace() != null) {
            map.put("space", config.getSpace().getValue());
        }
        if (config.getSourceKey() != null) {
            map.put("source_key", config.getSourceKey());
        }
        putIfNotNull(map, "hnsw", toHnswIndexConfigMap(config.getHnsw()));
        putIfNotNull(map, "spann", toSpannIndexConfigMap(config.getSpann()));
        putIfNotNull(map, "embedding_function", toEmbeddingFunctionSpecMap(config.getEmbeddingFunction()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toSparseVectorIndexConfigMap(SparseVectorIndexConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (config.getSourceKey() != null) {
            map.put("source_key", config.getSourceKey());
        }
        if (config.getBm25() != null) {
            map.put("bm25", config.getBm25());
        }
        putIfNotNull(map, "embedding_function", toEmbeddingFunctionSpecMap(config.getEmbeddingFunction()));
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toFtsIndexConfigMap(FtsIndexConfig config) {
        if (config == null) {
            return null;
        }
        return new LinkedHashMap<String, Object>();
    }

    private static Map<String, Object> toStringInvertedIndexConfigMap(StringInvertedIndexConfig config) {
        if (config == null) {
            return null;
        }
        return new LinkedHashMap<String, Object>();
    }

    private static Map<String, Object> toIntInvertedIndexConfigMap(IntInvertedIndexConfig config) {
        if (config == null) {
            return null;
        }
        return new LinkedHashMap<String, Object>();
    }

    private static Map<String, Object> toFloatInvertedIndexConfigMap(FloatInvertedIndexConfig config) {
        if (config == null) {
            return null;
        }
        return new LinkedHashMap<String, Object>();
    }

    private static Map<String, Object> toBoolInvertedIndexConfigMap(BoolInvertedIndexConfig config) {
        if (config == null) {
            return null;
        }
        return new LinkedHashMap<String, Object>();
    }

    private static Map<String, Object> toHnswIndexConfigMap(HnswIndexConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "ef_construction", config.getEfConstruction());
        putIfNotNull(map, "max_neighbors", config.getMaxNeighbors());
        putIfNotNull(map, "ef_search", config.getEfSearch());
        putIfNotNull(map, "num_threads", config.getNumThreads());
        putIfNotNull(map, "batch_size", config.getBatchSize());
        putIfNotNull(map, "sync_threshold", config.getSyncThreshold());
        putIfNotNull(map, "resize_factor", config.getResizeFactor());
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toSpannIndexConfigMap(SpannIndexConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        putIfNotNull(map, "search_nprobe", config.getSearchNprobe());
        putIfNotNull(map, "search_rng_factor", config.getSearchRngFactor());
        putIfNotNull(map, "search_rng_epsilon", config.getSearchRngEpsilon());
        putIfNotNull(map, "nreplica_count", config.getNreplicaCount());
        putIfNotNull(map, "write_rng_factor", config.getWriteRngFactor());
        putIfNotNull(map, "write_rng_epsilon", config.getWriteRngEpsilon());
        putIfNotNull(map, "split_threshold", config.getSplitThreshold());
        putIfNotNull(map, "num_samples_kmeans", config.getNumSamplesKmeans());
        putIfNotNull(map, "initial_lambda", config.getInitialLambda());
        putIfNotNull(map, "reassign_neighbor_count", config.getReassignNeighborCount());
        putIfNotNull(map, "merge_threshold", config.getMergeThreshold());
        putIfNotNull(map, "num_centers_to_merge_to", config.getNumCentersToMergeTo());
        putIfNotNull(map, "write_nprobe", config.getWriteNprobe());
        putIfNotNull(map, "ef_construction", config.getEfConstruction());
        putIfNotNull(map, "ef_search", config.getEfSearch());
        putIfNotNull(map, "max_neighbors", config.getMaxNeighbors());
        if (config.getQuantize() != null) {
            map.put("quantize", config.getQuantize().getValue());
        }
        return map.isEmpty() ? null : map;
    }

    private static Map<String, Object> toCmekMap(Cmek cmek) {
        if (cmek == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(cmek.getProvider().getValue(), cmek.getResource());
        return map;
    }

    private static Map<String, Object> toEmbeddingFunctionSpecMap(EmbeddingFunctionSpec spec) {
        if (spec == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (spec.getType() != null) {
            map.put("type", spec.getType());
        }
        map.put("name", spec.getName());
        if (spec.getConfig() != null && !spec.getConfig().isEmpty()) {
            map.put("config", new LinkedHashMap<String, Object>(spec.getConfig()));
        }
        return map;
    }

    private static void parsePositiveInt(Map<String, Object> config,
                                         String key,
                                         CollectionConfiguration.Builder ignored,
                                         IntConsumer consumer) {
        Object value = config.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            throw invalidConfiguration(key + " must be numeric");
        }
        int intValue = ((Number) value).intValue();
        try {
            consumer.accept(intValue);
        } catch (IllegalArgumentException e) {
            throw invalidConfiguration(key + " must be > 0 but was " + intValue, e);
        }
    }

    private static void parseAtLeastInt(Map<String, Object> config,
                                        String key,
                                        int min,
                                        CollectionConfiguration.Builder ignored,
                                        IntConsumer consumer) {
        Object value = config.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            throw invalidConfiguration(key + " must be numeric");
        }
        int intValue = ((Number) value).intValue();
        try {
            consumer.accept(intValue);
        } catch (IllegalArgumentException e) {
            throw invalidConfiguration(key + " must be >= " + min + " but was " + intValue, e);
        }
    }

    private static void parsePositiveFiniteDouble(Map<String, Object> config,
                                                   String key,
                                                   CollectionConfiguration.Builder ignored,
                                                   DoubleConsumer consumer) {
        Object value = config.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            throw invalidConfiguration(key + " must be numeric");
        }
        double doubleValue = ((Number) value).doubleValue();
        try {
            consumer.accept(doubleValue);
        } catch (IllegalArgumentException e) {
            throw invalidConfiguration(key + " must be > 0 and finite but was " + doubleValue, e);
        }
    }

    private static void parseInt(Map<String, Object> map,
                                 String key,
                                 String fieldName,
                                 IntConsumer consumer) {
        Object value = map.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(fieldName + "." + key + " must be numeric");
        }
        consumer.accept(((Number) value).intValue());
    }

    private static void parseDouble(Map<String, Object> map,
                                    String key,
                                    String fieldName,
                                    DoubleConsumer consumer) {
        Object value = map.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(fieldName + "." + key + " must be numeric");
        }
        consumer.accept(((Number) value).doubleValue());
    }

    private static boolean requireBoolean(Object raw, String fieldName) {
        if (!(raw instanceof Boolean)) {
            throw new IllegalArgumentException(fieldName + " must be boolean");
        }
        return ((Boolean) raw).booleanValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireMap(Object raw, String fieldName) {
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException(fieldName + " must be an object");
        }
        Map<?, ?> wildMap = (Map<?, ?>) raw;
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : wildMap.entrySet()) {
            Object rawKey = entry.getKey();
            if (!(rawKey instanceof String)) {
                throw new IllegalArgumentException(fieldName + " contains non-string key: " + rawKey);
            }
            map.put((String) rawKey, entry.getValue());
        }
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static ChromaDeserializationException invalidConfiguration(String message) {
        return new ChromaDeserializationException(
                "Server returned invalid collection configuration: " + message,
                200
        );
    }

    private static ChromaDeserializationException invalidConfiguration(String message, Throwable cause) {
        return new ChromaDeserializationException(
                "Server returned invalid collection configuration: " + message,
                200,
                cause
        );
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private interface DoubleConsumer {
        void accept(double value);
    }
}
