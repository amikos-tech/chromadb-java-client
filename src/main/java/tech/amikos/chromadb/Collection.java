package tech.amikos.chromadb;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Collection {
    static Gson gson = new Gson();
    DefaultApi api;

    Client client;
    String collectionName;

    String collectionId;

    LinkedTreeMap<String, Object> metadata = new LinkedTreeMap<>();

    private EmbeddingFunction embeddingFunction;


    public Collection(DefaultApi api,String collectionName, EmbeddingFunction embeddingFunction) {
        this.api = api;
        this.collectionName = collectionName;
        this.embeddingFunction = embeddingFunction;

    }

    public Collection(DefaultApi api,Client client, String collectionName, EmbeddingFunction embeddingFunction) {
        this.api = api;
        this.client = client;
        this.collectionName = collectionName;
        this.embeddingFunction = embeddingFunction;

    }



    public Collection name(String collectionName){
        this.collectionName = collectionName;
        return this;
    }

    public String getName() {
        return collectionName;
    }

    public String getId() {
        return collectionId;
    }

    public Collection metadata(String key,String value){
        metadata.put(key,value);
        return this;
    }
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Collection ef(EmbeddingFunction embeddingFunction){
        this.embeddingFunction = embeddingFunction;
        return this;
    }

    public Collection createOrGet(){
        return client.createCollection(this.collectionName,this.metadata,true,this.embeddingFunction);
    }

    public Collection create(){
        return client.createCollection(this.collectionName,this.metadata,false,this.embeddingFunction);
    }

    public Collection fetch() throws ApiException {
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

    public Object update(){
        return this.update(this.collectionName,this.metadata);
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

    public Collection remove(){
        return client.deleteCollection(this.collectionName);
    }



    public static Collection getInstance(DefaultApi api, String collectionName) throws ApiException {
        return new Collection(api,collectionName, null);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "collectionName='" + collectionName + '\'' +
                ", collectionId='" + collectionId + '\'' +
                ", metadata=" + metadata +
                '}';
    }


    public Embedding newEmbedding(){
        return new Embedding(this);
    }

    public Object add(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        AddEmbedding req = new AddEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.setEmbeddings((List<Object>) (Object) _embeddings);
        req.setMetadatas((List<Map<String, Object>>) (Object) metadatas);
        req.setDocuments(documents);
        req.incrementIndex(true);
        req.setIds(ids);
        return api.add(req, this.collectionId);
    }

    public GetResult get() throws ApiException {
        return this.get(null, null, null);
    }

    public GetResult get(List<String> ids, Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        GetEmbedding req = new GetEmbedding();
        req.ids(ids).where(where).whereDocument(whereDocument);
        Gson gson = new Gson();
        String json = gson.toJson(api.get(req, this.collectionId));
        return new Gson().fromJson(json, GetResult.class);
    }


    public Object upsert(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        AddEmbedding req = new AddEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.setEmbeddings((List<Object>) (Object) _embeddings);
        req.setMetadatas((List<Map<String, Object>>) (Object) metadatas);
        req.setDocuments(documents);
        req.incrementIndex(true);
        req.setIds(ids);
        return api.upsert(req, this.collectionId);
    }

    public Object updateEmbeddings(List<List<Float>> embeddings, List<Map<String, String>> metadatas, List<String> documents, List<String> ids) throws ApiException {
        UpdateEmbedding req = new UpdateEmbedding();
        List<List<Float>> _embeddings = embeddings;
        if (_embeddings == null) {
            _embeddings = this.embeddingFunction.createEmbedding(documents);
        }
        req.setEmbeddings((List<Object>) (Object) _embeddings);
        req.setDocuments(documents);
        req.setMetadatas((List<Object>) (Object) metadatas);
        req.setIds(ids);
        return api.update(req, this.collectionId);
    }

    public Object delete() throws ApiException {
        return this.delete(null, null, null);
    }

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

    public Object deleteWithIds(List<String> ids) throws ApiException {
        return delete(ids, null, null);
    }

    public Object deleteWhere(Map<String, String> where) throws ApiException {
        return delete(null, where, null);
    }

    public Object deleteWhereWhereDocuments(Map<String, String> where, Map<String, Object> whereDocument) throws ApiException {
        return delete(null, where, whereDocument);
    }

    public Object deleteWhereDocuments(Map<String, Object> whereDocument) throws ApiException {
        return delete(null, null, whereDocument);
    }

    public Integer count() throws ApiException {
        return api.count(this.collectionId);
    }
    @Deprecated
    public Boolean createIndex() throws ApiException {
        return (Boolean) api.createIndex(this.collectionId);
    }

    public Query newQuery(){
        return new Query();
    }

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
