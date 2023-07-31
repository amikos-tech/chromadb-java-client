package tech.amikos.chromadb;

import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.handler.ApiClient;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.CreateCollection;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ChromaDB Client
 */
public class Client {
    final ApiClient apiClient = new ApiClient();

    DefaultApi api;

    public Client(String basePath) {
        apiClient.setBasePath(basePath);
        api = new DefaultApi(apiClient);
    }

    public Collection getCollection(String collectionName, EmbeddingFunction embeddingFunction) {
        return new Collection(api, collectionName, embeddingFunction);
    }

    public Map<String, BigDecimal> heartbeat() throws ApiException {
        return api.heartbeat();
    }

    public Collection createCollection(String collectionName, Map<String, String> metadata, Boolean createOrGet, EmbeddingFunction embeddingFunction) throws ApiException {
        CreateCollection req = new CreateCollection();
        req.setName(collectionName);
        req.setMetadata(metadata);
        req.setGetOrCreate(createOrGet);
        LinkedTreeMap resp = (LinkedTreeMap) api.createCollection(req);
        return new Collection(api, (String) resp.get("name"), embeddingFunction);
    }

    public Collection deleteCollection(String collectionName) throws ApiException {
        Collection collection = Collection.getInstance(api,collectionName);
        collection.delete();
        return collection;
    }

    public Collection upsert(String collectionName, EmbeddingFunction ef) {
        Collection collection = getCollection(collectionName, ef);
//        collection.upsert();
        return collection;
    }
}
