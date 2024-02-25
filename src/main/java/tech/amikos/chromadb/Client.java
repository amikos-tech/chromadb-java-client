package tech.amikos.chromadb;

import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.handler.ApiClient;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.handler.DefaultApi;
import tech.amikos.chromadb.model.*;

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

    private String tenant = "default_tenant";
    private String database = "default_database";

    private Boolean preFlightChecksCompleted = false;
    private String apiVersion;
    private Integer max_batch_size;

    public Client(String basePath) {
        apiClient.setBasePath(basePath);
        api = new DefaultApi(apiClient);
    }

    private void preFlightChecks() throws ApiException {
        if (this.preFlightChecksCompleted) {
            return;
        }
        this.apiVersion = this.version();
        Utils.SemanticVersion semVer = new Utils.SemanticVersion(this.apiVersion);
        if (semVer.compareTo(new Utils.SemanticVersion("0.4.15")) < 0) {
            return;
        } else {
            this.getTenant(this.tenant);
            this.getDatabase(this.database, this.tenant);
            PreflightChecks preFlightchecks = this.api.preFlightChecks();
            this.max_batch_size = Double.valueOf(preFlightchecks.get("max_batch_size").toString()).intValue();
        }
        this.preFlightChecksCompleted = true;
    }

    public void setActiveTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setActiveDatabase(String database) {
        this.database = database;
    }

    public String getActiveTenant() {
        return this.tenant;
    }

    public String getActiveDatabase() {
        return this.database;
    }

    public Collection getCollection(String collectionName, EmbeddingFunction embeddingFunction) throws ApiException {
        preFlightChecks();
        return new Collection(api, collectionName, embeddingFunction, this.tenant, this.database).fetch();
    }

    public Map<String, BigDecimal> heartbeat() throws ApiException {
        return api.heartbeat();
    }

    public Collection createCollection(String collectionName, Map<String, Object> metadata, Boolean createOrGet, EmbeddingFunction embeddingFunction) throws ApiException {
        preFlightChecks();
        CreateCollection req = new CreateCollection();
        req.setName(collectionName);
        Map<String, Object> _metadata = metadata;
        if (metadata == null || metadata.isEmpty()) {
            _metadata = new LinkedTreeMap<>();
        }
        if (!_metadata.containsKey("embedding_function")) {
            _metadata.put("embedding_function", embeddingFunction.getClass().getName());
        }
        req.setMetadata(_metadata);
        req.setGetOrCreate(createOrGet);
        tech.amikos.chromadb.model.Collection c = api.createCollection(req, this.tenant, this.database);
        return new Collection(api, (String) c.getName(), embeddingFunction, this.tenant, this.database).fetch();
    }

    public CollectionBuilder createCollectionWithBuilder(String collectionName) {
        return CollectionBuilder.instance(this, collectionName);
    }

    public Collection deleteCollection(String collectionName) throws ApiException {
        preFlightChecks();
        Collection collection = Collection.getInstance(api, collectionName);
        api.deleteCollection(collectionName, this.tenant, this.database);
        return collection;
    }

    public Boolean reset() throws ApiException {
        return api.reset();
    }

    public Tenant createTenant(String tenant) throws ApiException {
        return api.createTenant(new CreateTenant().name(tenant));
    }

    /**
     * Gets tenant from Chroma
     *
     * @param tenant name
     * @return tenant name
     */
    public Tenant getTenant(String tenant) throws ApiException {
        return api.getTenant(tenant);
    }

    /**
     * Creates database in Chroma for the active tenant
     *
     * @param database name
     * @return database name
     */
    public Database createDatabase(String database) throws ApiException {
        return api.createDatabase(new CreateDatabase().name(database), this.tenant);
    }

    /**
     * Creates database in Chroma for given tenant
     *
     * @param database name
     * @param tenant   name. If null, the active tenant will be used
     * @return database name
     */
    public Database createDatabase(String database, String tenant) throws ApiException {
        String dbTenant = tenant != null ? tenant : this.tenant;
        return api.createDatabase(new CreateDatabase().name(database), dbTenant);
    }

    /**
     * Gets database from Chroma for given tenant
     *
     * @param database name
     * @param tenant   name. If null, the active tenant will be used
     * @return database name
     */
    public Database getDatabase(String database, String tenant) throws ApiException {
        String dbTenant = tenant != null ? tenant : this.tenant;
        return api.getDatabase(database, dbTenant);
    }

    /**
     * Gets database from Chroma for the active tenant
     *
     * @param database name
     * @return database name
     */
    public Database getDatabase(String database) throws ApiException {
        return api.getDatabase(database, this.tenant);
    }

    public List<Collection> listCollections() throws ApiException {
        preFlightChecks();
        List<tech.amikos.chromadb.model.Collection> apiResponse = api.listCollections(this.tenant, this.database);
        return apiResponse.stream().map((tech.amikos.chromadb.model.Collection m) -> {
            try {
                return getCollection(m.getName(), null);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public Integer countCollections() throws ApiException {
        preFlightChecks();
        return api.countCollections(this.tenant, this.database);
    }

    public String version() throws ApiException {
        return api.version();
    }
}
