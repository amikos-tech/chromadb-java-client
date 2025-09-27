package tech.amikos.chromadb;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class CollectionImpl {
    static Gson gson = new Gson();
    DefaultApi api;
    String collectionName;

    String collectionId;

    LinkedTreeMap<String, Object> metadata = new LinkedTreeMap<>();

    private EmbeddingFunction embeddingFunction;

    public CollectionImpl(DefaultApi api, String collectionName, EmbeddingFunction embeddingFunction) {
        this.api = api;
        this.collectionName = collectionName;
        this.embeddingFunction = embeddingFunction;

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

    public CollectionImpl fetch() throws ApiException {
        try {
            LinkedTreeMap<String, ?> resp = (LinkedTreeMap<String, ?>) api.getCollection(collectionName);
            this.collectionName = resp.get("name").toString();
            this.collectionId = resp.get("id").toString();
            this.metadata = (LinkedTreeMap<String, Object>) resp.get("metadata");
            return this;
        } catch (ApiException e) {
            throw e;
        }
    }

    public static CollectionImpl getInstance(DefaultApi api, String collectionName) throws ApiException {
        return new CollectionImpl(api, collectionName, null);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "collectionName='" + collectionName + '\'' +
                ", collectionId='" + collectionId + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    public GetResult get(List<String> ids, Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        GetEmbedding req = new GetEmbedding();
        req.ids(ids).where(where).whereDocument(whereDocument);
        Gson gson = new Gson();
        String json = gson.toJson(api.get(req, this.collectionId));
        return new Gson().fromJson(json, GetResult.class);
    }

    public GetResult get() throws ApiException {
        return this.get(null, null, null);
    }

    public Object delete() throws ApiException {
        return this.delete(null, null, null);
    }

    public Object upsert(List<Embedding> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ChromaException {
        AddEmbedding req = new AddEmbedding();
        List<Embedding> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.embedDocuments(documents);
        }
        req.setEmbeddings(_embeddings.stream().map(Embedding::asArray).collect(Collectors.toList()));
        req.setMetadatas((List<Map<String, Object>>) (Object) metadatas);
        req.setDocuments(documents);
        req.incrementIndex(true);
        req.setIds(ids);
        try {
            return api.upsert(req, this.collectionId);
        } catch (ApiException e) {
            throw new ChromaException(e);
        }
    }


    public Object add(List<Embedding> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ChromaException {
        AddEmbedding req = new AddEmbedding();
        List<Embedding> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.embedDocuments(documents);
        }
        req.setEmbeddings(_embeddings.stream().map(Embedding::asArray).collect(Collectors.toList()));
        req.setMetadatas((List<Map<String, Object>>) (Object) metadatas);
        req.setDocuments(documents);
        req.incrementIndex(true);
        req.setIds(ids);
        try {
            return api.add(req, this.collectionId);
        } catch (ApiException e) {
            throw new ChromaException(e);
        }
    }

    public Integer count() throws ApiException {
        return api.count(this.collectionId);
    }

    public Object delete(List<String> ids, Map<String, Object> where, Map<String, Object> whereDocument) throws ApiException {
        DeleteEmbedding req = new DeleteEmbedding();
        req.setIds(ids);
        if (where != null) {
            req.where(where.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        if (whereDocument != null) {
            req.whereDocument(whereDocument.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return api.delete(req, this.collectionId);
    }

    public Object deleteWithIds(List<String> ids) throws ApiException {
        return delete(ids, null, null);
    }

    public Object deleteWhere(Map<String, Object> where) throws ApiException {
        return delete(null, where, null);
    }

    public Object deleteWhereWhereDocuments(Map<String, Object> where, Map<String, Object> whereDocument) throws ApiException {
        return delete(null, where, whereDocument);
    }

    public Object deleteWhereDocuments(Map<String, Object> whereDocument) throws ApiException {
        return delete(null, null, whereDocument);
    }


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

    public Object updateEmbeddings(List<Embedding> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ChromaException {
        UpdateEmbedding req = new UpdateEmbedding();
        List<Embedding> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.embedDocuments(documents);
        }
        req.setEmbeddings(_embeddings.stream().map(Embedding::asArray).collect(Collectors.toList()));
        req.setDocuments(documents);
        req.setMetadatas((List<Object>) (Object) metadatas);
        req.setIds(ids);
        try {
            return api.update(req, this.collectionId);
        } catch (ApiException e) {
            throw new ChromaException(e);
        }
    }


    public QueryResponse query(List<String> queryTexts, Integer nResults, Map<String, Object> where, Map<String, Object> whereDocument, List<QueryEmbedding.IncludeEnum> include) throws ChromaException {
        QueryEmbedding body = new QueryEmbedding();
        body.queryEmbeddings(this.embeddingFunction.embedDocuments(queryTexts).stream().map(Embedding::asArray).collect(Collectors.toList()));
        body.nResults(nResults);
        body.include(include);
        if (where != null) {
            body.where(where.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        if (whereDocument != null) {
            body.whereDocument(whereDocument.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        try {
            Gson gson = new Gson();
            String json = gson.toJson(api.getNearestNeighbors(body, this.collectionId));
            return new Gson().fromJson(json, QueryResponse.class);
        } catch (ApiException e) {
            throw new ChromaException(e);
        }
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
        private List<String> documents;
        @SerializedName("embeddings")
        private List<Float> embeddings;
        @SerializedName("ids")
        private List<String> ids;
        @SerializedName("metadatas")
        private List<Map<String, Object>> metadatas;

        public List<String> getDocuments() {
            return documents;
        }

        public List<Float> getEmbeddings() {
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
