package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Package-private JSON request/response DTOs for Gson serialization.
 */
final class ChromaDtos {

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
        @SerializedName("get_or_create")
        final boolean getOrCreate;

        CreateCollectionRequest(String name, Map<String, Object> metadata,
                                Map<String, Object> configuration, boolean getOrCreate) {
            this.name = name;
            this.metadata = metadata;
            this.configuration = configuration;
            this.getOrCreate = getOrCreate;
        }
    }

    static final class UpdateCollectionRequest {
        @SerializedName("new_name")
        final String newName;
        @SerializedName("new_metadata")
        final Map<String, Object> newMetadata;

        UpdateCollectionRequest(String newName, Map<String, Object> newMetadata) {
            this.newName = newName;
            this.newMetadata = newMetadata;
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
        if (config.getHnswBatchSize() != null) {
            params.put("hnsw:batch_size", config.getHnswBatchSize());
        }
        if (config.getHnswSyncThreshold() != null) {
            params.put("hnsw:sync_threshold", config.getHnswSyncThreshold());
        }
        if (params.isEmpty()) {
            return null;
        }
        return params;
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
        Object m = configJson.get("hnsw:M");
        if (m != null) {
            if (!(m instanceof Number)) {
                throw invalidConfiguration("hnsw:M must be numeric");
            }
            int value = ((Number) m).intValue();
            try {
                builder.hnswM(value);
            } catch (IllegalArgumentException e) {
                throw invalidConfiguration("hnsw:M must be > 0 but was " + value, e);
            }
        }
        Object constructionEf = configJson.get("hnsw:construction_ef");
        if (constructionEf != null) {
            if (!(constructionEf instanceof Number)) {
                throw invalidConfiguration("hnsw:construction_ef must be numeric");
            }
            int value = ((Number) constructionEf).intValue();
            try {
                builder.hnswConstructionEf(value);
            } catch (IllegalArgumentException e) {
                throw invalidConfiguration("hnsw:construction_ef must be > 0 but was " + value, e);
            }
        }
        Object searchEf = configJson.get("hnsw:search_ef");
        if (searchEf != null) {
            if (!(searchEf instanceof Number)) {
                throw invalidConfiguration("hnsw:search_ef must be numeric");
            }
            int value = ((Number) searchEf).intValue();
            try {
                builder.hnswSearchEf(value);
            } catch (IllegalArgumentException e) {
                throw invalidConfiguration("hnsw:search_ef must be > 0 but was " + value, e);
            }
        }
        Object batchSize = configJson.get("hnsw:batch_size");
        if (batchSize != null) {
            if (!(batchSize instanceof Number)) {
                throw invalidConfiguration("hnsw:batch_size must be numeric");
            }
            int value = ((Number) batchSize).intValue();
            try {
                builder.hnswBatchSize(value);
            } catch (IllegalArgumentException e) {
                throw invalidConfiguration("hnsw:batch_size must be > 0 but was " + value, e);
            }
        }
        Object syncThreshold = configJson.get("hnsw:sync_threshold");
        if (syncThreshold != null) {
            if (!(syncThreshold instanceof Number)) {
                throw invalidConfiguration("hnsw:sync_threshold must be numeric");
            }
            int value = ((Number) syncThreshold).intValue();
            try {
                builder.hnswSyncThreshold(value);
            } catch (IllegalArgumentException e) {
                throw invalidConfiguration("hnsw:sync_threshold must be > 0 but was " + value, e);
            }
        }
        return builder.build();
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
}
