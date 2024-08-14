package tech.amikos.chromadb;

import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.handler.ApiClient;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.CreateCollection;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChromaDB Client
 */
public class Client {
    final ApiClient apiClient = new ApiClient();
    private int timeout = 60;
    DefaultApi api;

    public Client(String basePath) {
        apiClient.setBasePath(basePath);
        api = new DefaultApi(apiClient);
        apiClient.setHttpClient(apiClient.getHttpClient().newBuilder()
                .readTimeout(this.timeout, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(this.timeout, java.util.concurrent.TimeUnit.SECONDS)
                .build());
        api.getApiClient().setUserAgent("Chroma-JavaClient/0.1.x");
    }

    /**
     * Set the timeout for the client
     * @param timeout timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        apiClient.setHttpClient(apiClient.getHttpClient().newBuilder()
                .readTimeout(this.timeout, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(this.timeout, java.util.concurrent.TimeUnit.SECONDS)
                .build());
    }

    /**
     * Set the default headers for the client to be sent with every request
     * @param headers
     */
    public void setDefaultHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            apiClient.addDefaultHeader(entry.getKey(), entry.getValue());
        }
    }

    public Collection getCollection(String collectionName, EmbeddingFunction embeddingFunction) throws ApiException {
        return new Collection(api, collectionName, embeddingFunction).fetch();
    }

    public Map<String, BigDecimal> heartbeat() throws ApiException {
        return api.heartbeat();
    }

    public Collection createCollection(String collectionName, Map<String, String> metadata, Boolean createOrGet, EmbeddingFunction embeddingFunction) throws ApiException {
        CreateCollection req = new CreateCollection();
        req.setName(collectionName);
        Map<String, String> _metadata = metadata;
        if (metadata == null || metadata.isEmpty() || !metadata.containsKey("embedding_function")) {
            _metadata = new LinkedTreeMap<>();
            _metadata.put("embedding_function", embeddingFunction.getClass().getName());
        }
        req.setMetadata(_metadata);
        req.setGetOrCreate(createOrGet);
        LinkedTreeMap resp = (LinkedTreeMap) api.createCollection(req);
        return new Collection(api, (String) resp.get("name"), embeddingFunction).fetch();
    }

    public Collection deleteCollection(String collectionName) throws ApiException {
        Collection collection = Collection.getInstance(api, collectionName);
        api.deleteCollection(collectionName);
        return collection;
    }

    public Collection upsert(String collectionName, EmbeddingFunction ef) throws ApiException {
        Collection collection = getCollection(collectionName, ef);
//        collection.upsert();
        return collection;
    }

    public Boolean reset() throws ApiException {
        return api.reset();
    }

    public List<Collection> listCollections() throws ApiException {
        List<LinkedTreeMap> apiResponse = (List<LinkedTreeMap>) api.listCollections();
        return apiResponse.stream().map((LinkedTreeMap m) -> {
            try {
                return getCollection((String) m.get("name"), null);
            } catch (ApiException e) {
                e.printStackTrace(); //this is not great as we're swallowing the exception
            }
            return null;
        }).collect(Collectors.toList());
    }

    public String version() throws ApiException {
        return api.version();
    }
}
