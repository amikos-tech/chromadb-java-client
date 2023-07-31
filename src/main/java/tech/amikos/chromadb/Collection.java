package tech.amikos.chromadb;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Response;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.AddEmbedding;
import tech.amikos.chromadb.model.GetEmbedding;
import tech.amikos.chromadb.model.QueryEmbedding;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class Collection {
    static Gson gson = new Gson();
    DefaultApi api;
    String collectionName;

    String collectionId;

    LinkedTreeMap<String, Object> metadata = new LinkedTreeMap<>();

    private EmbeddingFunction embeddingFunction;

    public Collection(DefaultApi api, String collectionName, EmbeddingFunction embeddingFunction) throws ApiException {
        this.api = api;
        this.collectionName = collectionName;
        this.embeddingFunction = embeddingFunction;
        try {
            LinkedTreeMap<String, ?> resp = (LinkedTreeMap<String, ?>) api.getCollection(collectionName);
            this.collectionName = resp.get("name").toString();
            this.collectionId = resp.get("id").toString();
            this.metadata = (LinkedTreeMap<String, Object>) resp.get("metadata");
        } catch (ApiException e) {
            throw e;
        }
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

    public HashMap<String, Object> get() {
        try {
            Call r = api.getCall(new GetEmbedding(), this.collectionId, null, null);
            Response c = r.execute();


            // Define the Type for the HashMap
            Type type = new TypeToken<HashMap<String, Object>>() {
            }.getType();

            // Parse the JSON string to a HashMap
            HashMap<String, Object> hashMap = gson.fromJson(c.body().string(), type);
            return hashMap;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() throws ApiException {
        api.deleteCollection(this.collectionName);
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

    public LinkedTreeMap<String, Object> query(List<String> queryTexts, Integer nResults, Map<String, String> where, Map<String, String> whereDocument, List<QueryEmbedding.IncludeEnum> include) {
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
        try {
            return (LinkedTreeMap<String, Object>) api.getNearestNeighbors(body, this.collectionId);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
