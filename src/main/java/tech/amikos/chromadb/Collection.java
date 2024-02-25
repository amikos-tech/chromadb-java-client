package tech.amikos.chromadb;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Collection {
    static Gson gson = new Gson();
    private DefaultApi api;
    private String collectionName;

    private String collectionId;

    private Map<String, Object> metadata = new LinkedTreeMap<>();
    private Map<String, Object> preFlightChecks;
    private Integer maxBatchSize;

    private EmbeddingFunction embeddingFunction;
    private String tenant;
    private String database;

    public Collection(DefaultApi api, String collectionName, EmbeddingFunction embeddingFunction) throws ApiException {
        this(api, collectionName, embeddingFunction, null, null);
    }

    public Collection(DefaultApi api, String collectionName, EmbeddingFunction embeddingFunction, String tenant, String database) throws ApiException {
        this.tenant = tenant != null ? tenant : "default_tenant";
        this.database = database != null ? database : "default_database";
        this.api = api;
        this.collectionName = collectionName;
        this.embeddingFunction = embeddingFunction;

        this.preFlightChecks = this.api.preFlightChecks();
        this.maxBatchSize = this.preFlightChecks.get("max_batch_size") != null ? ((Double) this.preFlightChecks.get("max_batch_size")).intValue() : 1000;
    }


    public String getName() {
        return collectionName;
    }

    public String getId() {
        return collectionId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Collection fetch() throws ApiException {
        tech.amikos.chromadb.model.Collection resp = api.getCollection(collectionName, this.tenant, this.database);
        this.collectionName = resp.getName();
        this.collectionId = String.valueOf(resp.getId());
        this.metadata = resp.getMetadata();
        return this;
    }

    public static Collection getInstance(DefaultApi api, String collectionName) throws ApiException {
        return new Collection(api, collectionName, null);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "collectionName='" + collectionName + '\'' +
                ", collectionId='" + collectionId + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    /**
     * @deprecated This method is deprecated in favor of get with GetEmbedding request.
     * Use {@link #get(GetEmbedding)} instead.
     */
    @Deprecated
    public GetResult get(List<String> ids, Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        GetEmbedding req = new GetEmbedding();
        req.ids(ids).where(where).whereDocument(whereDocument);
        Gson gson = new Gson();
        String json = gson.toJson(api.get(req, this.collectionId));
        return new Gson().fromJson(json, GetResult.class);
    }

    public GetResult get(GetEmbedding req) throws ApiException {
        tech.amikos.chromadb.model.GetResult resp = api.get(req, this.collectionId);
        return GetResult.fromAPIObject(resp);
    }

    /**
     * @deprecated This method is deprecated in favor of get with GetEmbedding request.
     * Use {@link #get(GetEmbedding)} instead.
     */
    @Deprecated
    public GetResult get() throws ApiException {
        return this.get(null, null, null);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with DeleteEmbedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object delete() throws ApiException {
        return this.delete(null, null, null);
    }

    /**
     * @deprecated This method is deprecated in favor of upsert with AddEmbedding request.
     * Use {@link #upsert(AddEmbedding)} instead.
     */
    @Deprecated
    public Object upsert(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        AddEmbedding req = new AddEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.embeddings((List<Object>) (Object) _embeddings);
        req.setMetadatas(metadatas.stream()
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue())))
                .collect(Collectors.toList()));
        req.setDocuments(documents);
        req.setIds(ids);
        return api.upsert(req, this.collectionId);
    }

    public Object upsert(AddEmbedding req) throws ApiException {
        if (req.getIds() == null) {
            throw new RuntimeException("Ids must be provided");
        }
        if (req.getDocuments() == null && req.getEmbeddings() == null) {
            throw new RuntimeException("At least one of documents or embeddings must be provided");
        }
        if (req.getDocuments() != null && req.getEmbeddings() == null) {
            req.embeddings(Arrays.asList(this.embeddingFunction.createEmbedding(req.getDocuments()).toArray()));
        }
        return api.upsert(req, this.collectionId);
    }


    /**
     * @deprecated This method is deprecated in favor of add with add embedding request.
     * Use {@link #add(AddEmbedding)} instead.
     */
    @Deprecated
    public Object add(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        AddEmbedding req = new AddEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.setEmbeddings((List<Object>) (Object) _embeddings);
        req.setMetadatas(metadatas.stream()
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue())))
                .collect(Collectors.toList()));
        req.setDocuments(documents);
        req.setIds(ids);
        return api.add(req, this.collectionId);
    }


    public Object add(AddEmbedding req) throws ApiException {
        if (req.getIds() == null) {
            throw new RuntimeException("Ids must be provided");
        }
        if (req.getDocuments() == null && req.getEmbeddings() == null) {
            throw new RuntimeException("At least one of documents or embeddings must be provided");
        }
        if (req.getDocuments() != null && req.getEmbeddings() == null) {
            req.embeddings(Arrays.asList(this.embeddingFunction.createEmbedding(req.getDocuments()).toArray()));
        }
        return api.add(req, this.collectionId);
    }

    public Integer count() throws ApiException {
        return api.count(this.collectionId);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with DeleteEmbedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object delete(List<String> ids, Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        DeleteEmbedding req = new DeleteEmbedding();
        req.setIds(ids);
        if (where != null) {
            req.where(where.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue())));
        }
        if (whereDocument != null) {
            req.whereDocument(whereDocument.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue())));
        }
        return api.delete(req, this.collectionId);
    }

    public Object delete(DeleteEmbedding req) throws ApiException {
        return api.delete(req, this.collectionId);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with embedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object deleteWithIds(List<String> ids) throws ApiException {
        return delete(ids, null, null);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with embedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object deleteWhere(Map<String, String> where) throws ApiException {
        return delete(null, where, null);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with embedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object deleteWhereWhereDocuments(Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        return delete(null, where, whereDocument);
    }

    /**
     * @deprecated This method is deprecated in favor of delete with embedding request.
     * Use {@link #delete(DeleteEmbedding)} instead.
     */
    @Deprecated
    public Object deleteWhereDocuments(Map<String, Object> whereDocument) throws ApiException {
        return delete(null, null, whereDocument);
    }


    /**
     * @deprecated This method is deprecated in favor of update with embedding request.
     * Use {@link #update(UpdateCollection)} instead.
     */
    @Deprecated
    public Object update(String newName, Map<String, Object> newMetadata) throws ApiException {
        UpdateCollection req = new UpdateCollection();
        if (newName != null) {
            req.setNewName(newName);
        }
        if (newMetadata != null && embeddingFunction != null) {
            if (!newMetadata.containsKey("embedding_function")) {
                newMetadata.put("embedding_function", embeddingFunction.getClass().getName());
            }
            req.setNewMetadata(newMetadata);
        }
        Object resp = api.updateCollection(req, this.collectionId);
        this.collectionName = newName;
        this.fetch(); //do we really need to fetch?
        return resp;
    }

    public Object update(UpdateCollection req) throws ApiException {
        if (req.getNewName() == null && req.getNewMetadata() == null) {
            throw new RuntimeException("At least one of newName or newMetadata must be provided");
        }

        if (req.getNewMetadata() != null && embeddingFunction != null) {
            if (!req.getNewMetadata().containsKey("embedding_function")) {
                req.getNewMetadata().put("embedding_function", embeddingFunction.getClass().getName());
            }
        }
        Object resp = api.updateCollection(req, this.collectionId);
        this.collectionName = req.getNewName();
        this.fetch(); //do we really need to fetch?
        return resp;
    }

    /**
     * @deprecated This method is deprecated in favor of update with embedding request.
     * Use {@link #updateEmbeddings(UpdateEmbedding)} instead.
     */
    @Deprecated
    public Object updateEmbeddings(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        UpdateEmbedding req = new UpdateEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.setEmbeddings(_embeddings);
        req.setDocuments(documents);
        if (metadatas != null) {
            req.setMetadatas(metadatas.stream()
                    .map(map -> map.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue())))
                    .collect(Collectors.toList()));
        }
        req.setIds(ids);
        return api.update(req, this.collectionId);
    }

    public Object updateEmbeddings(UpdateEmbedding req) throws ApiException {
        if (req.getIds() == null || req.getIds().isEmpty()) {
            throw new RuntimeException("Ids must be provided");
        }
        if (req.getEmbeddings() == null && req.getDocuments() != null) {
            req.embeddings(this.embeddingFunction.createEmbedding(req.getDocuments()));
        }
        return api.update(req, this.collectionId);
    }


    /**
     * @deprecated This method is deprecated in favor of query with query embedding request.
     * Use {@link #query(QueryEmbedding)} instead.
     */
    @Deprecated
    public QueryResponse query(List<String> queryTexts, Integer nResults, Map<String, String> where, Map<String, String> whereDocument, List<QueryEmbedding.IncludeEnum> include) throws ApiException {
        QueryEmbedding body = new QueryEmbedding();
        body.queryEmbeddings((List<Object>) (Object) this.embeddingFunction.createEmbedding(queryTexts));
        body.nResults(nResults);
        body.include(include);
        if (where != null) {
            body.where(where.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue())));
        }
        if (whereDocument != null) {
            body.whereDocument(whereDocument.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue())));
        }
        Gson gson = new Gson();
        String json = gson.toJson(api.getNearestNeighbors(body, this.collectionId));
        return new Gson().fromJson(json, QueryResponse.class);
    }

    public QueryResponse query(QueryEmbedding req) throws ApiException {
        if (req.getNResults() == null) {
            req.nResults(10);
        }
        if (req.getNResults() < 0) {
            throw new RuntimeException("nResults must be greater than 0");
        }
        if (req.getQueryTexts() == null && req.getQueryEmbeddings() == null) {
            throw new RuntimeException("At least one of queryTexts or queryEmbeddings must be provided");
        }
        if (req.getQueryTexts() != null && req.getQueryEmbeddings() == null) {
            req.queryEmbeddings(this.embeddingFunction.createEmbedding(req.getQueryTexts()));
        }
        Gson gson = new Gson();
        String json = gson.toJson(api.getNearestNeighbors(req, this.collectionId));
        return new Gson().fromJson(json, QueryResponse.class);
    }

    public static class QueryResponse {
        @SerializedName("documents")
        private List<List<String>> documents;
        @SerializedName("embeddings")
        private List<List<Float>> embeddings;
        @SerializedName("ids")
        private List<List<String>> ids;
        @SerializedName("metadatas")
        private List<List<Map<String, Object>>> metadatas;
        @SerializedName("distances")
        private List<List<Float>> distances;

        public List<List<String>> getDocuments() {
            return documents;
        }

        public List<List<Float>> getEmbeddings() {
            return embeddings;
        }

        public List<List<String>> getIds() {
            return ids;
        }

        public List<List<Map<String, Object>>> getMetadatas() {
            return metadatas;
        }

        public List<List<Float>> getDistances() {
            return distances;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }


    }

    public static class GetResult {
        @SerializedName("documents")
        private List<String> documents = null;
        @SerializedName("embeddings")
        private List<List<BigDecimal>> embeddings = null;
        @SerializedName("ids")
        private List<String> ids;
        @SerializedName("metadatas")
        private List<Map<String, Object>> metadatas = null;

        public static GetResult fromAPIObject(tech.amikos.chromadb.model.GetResult resp) {
            GetResult result = new GetResult();
            result.documents = resp.getDocuments();
            result.embeddings = resp.getEmbeddings();
            result.ids = resp.getIds();
            result.metadatas = resp.getMetadatas();
            return result;
        }

        public List<String> getDocuments() {
            return documents;
        }

        public List<List<BigDecimal>> getEmbeddings() {
            return embeddings;
        }

        public List<String> getIds() {
            return ids;
        }

        public List<Map<String, Object>> getMetadatas() {
            return metadatas;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
