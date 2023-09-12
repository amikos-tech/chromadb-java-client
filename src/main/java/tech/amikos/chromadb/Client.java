package tech.amikos.chromadb;

import com.google.gson.internal.LinkedTreeMap;
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

    DefaultApi api;

    private Client(String basePath) {
        apiClient.setBasePath(basePath);
        api = new DefaultApi(apiClient);
    }


    public static enum DistanceFunction {
        L2,
        COSINE,
        IP
    }

    public static Client newClient(String basePath){
        return new Client(basePath);
    }

    public Boolean reset() throws ApiException {
        return api.reset();
    }

    public String version() throws ApiException {
        return api.version();
    }

    public Map<String, BigDecimal> heartbeat() throws ApiException {
        return api.heartbeat();
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


    public Collection newCollection(){
        return new Collection(api,this,null,null);
    }


    public Collection createCollection(String collectionName, Map<String, String> metadata, Boolean createOrGet, EmbeddingFunction embeddingFunction) throws ApiException {
        return this.createCollection(collectionName, metadata, createOrGet, embeddingFunction, DistanceFunction.L2);
    }


    public Collection createCollection(String collectionName, Map<String, String> metadata, Boolean createOrGet, EmbeddingFunction embeddingFunction, DistanceFunction distanceFunction) throws ApiException {
        CreateCollection req = new CreateCollection();
        req.setName(collectionName);
        Map<String, String> _metadata = metadata;
        if (metadata == null || metadata.isEmpty() || !metadata.containsKey("embedding_function")) {
            _metadata = new LinkedTreeMap<>();
            _metadata.put("embedding_function", embeddingFunction.getClass().getName());
        }
        _metadata.put("hnsw:space", distanceFunction.toString().toLowerCase());
        req.setMetadata(_metadata);
        req.setGetOrCreate(createOrGet);
        LinkedTreeMap resp = (LinkedTreeMap) api.createCollection(req);
        System.out.println(resp);
        return new Collection(api, (String) resp.get("name"), embeddingFunction).fetch();
    }





    public Collection getCollection(String collectionName, EmbeddingFunction embeddingFunction) throws ApiException {
        return new Collection(api, collectionName, embeddingFunction).fetch();
    }



    public Collection upsertCollection(String collectionName, EmbeddingFunction ef) throws ApiException {
        Collection collection = getCollection(collectionName, ef);
//        collection.upsert();
        return collection;
    }


    public Collection deleteCollection(String collectionName) throws ApiException {
        Collection collection = Collection.getInstance(api, collectionName);
        api.deleteCollection(collectionName);
        return collection;
    }




}
