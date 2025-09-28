package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.http.HttpClient;
import tech.amikos.chromadb.v2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseClient implements Client {
    protected final HttpClient httpClient;

    protected BaseClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Tenant createTenant(String name) {
        CreateTenantRequest request = new CreateTenantRequest(name);
        return httpClient.post("/api/v2/tenants", request, Tenant.class);
    }

    @Override
    public Tenant getTenant(String name) {
        String path = String.format("/api/v2/tenants/%s", name);
        return httpClient.get(path, Tenant.class);
    }

    @Override
    public void updateTenant(String name, Consumer<UpdateTenantRequest.Builder> configurator) {
        UpdateTenantRequest.Builder builder = UpdateTenantRequest.builder();
        if (configurator != null) {
            configurator.accept(builder);
        }
        UpdateTenantRequest request = builder.build();
        String path = String.format("/api/v2/tenants/%s", name);
        httpClient.patch(path, request, Void.class);
    }

    @Override
    public Database createDatabase(String tenant, String name) {
        CreateDatabaseRequest request = new CreateDatabaseRequest(name);
        String path = String.format("/api/v2/tenants/%s/databases", tenant);
        return httpClient.post(path, request, Database.class);
    }

    @Override
    public Database getDatabase(String tenant, String name) {
        String path = String.format("/api/v2/tenants/%s/databases/%s", tenant, name);
        return httpClient.get(path, Database.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Database> listDatabases(String tenant) {
        return listDatabases(tenant, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Database> listDatabases(String tenant, Integer limit, Integer offset) {
        String path = String.format("/api/v2/tenants/%s/databases", tenant);
        if (limit != null || offset != null) {
            path += buildQueryParams(limit, offset);
        }
        return (List<Database>) httpClient.get(path, List.class);
    }

    @Override
    public void deleteDatabase(String tenant, String name) {
        String path = String.format("/api/v2/tenants/%s/databases/%s", tenant, name);
        httpClient.delete(path, Void.class);
    }

    @Override
    public Collection createCollection(String tenant, String database, String name) {
        return createCollection(tenant, database, name, null);
    }

    @Override
    public Collection createCollection(String tenant, String database, String name,
                                       Consumer<CreateCollectionRequest.Builder> configurator) {
        CreateCollectionRequest.Builder builder = CreateCollectionRequest.builder(name);
        if (configurator != null) {
            configurator.accept(builder);
        }
        CreateCollectionRequest request = builder.build();
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections", tenant, database);
        CollectionModel model = httpClient.post(path, request, CollectionModel.class);
        return createCollectionInstance(model);
    }

    @Override
    public Collection getOrCreateCollection(String tenant, String database, String name) {
        return getOrCreateCollection(tenant, database, name, null);
    }

    @Override
    public Collection getOrCreateCollection(String tenant, String database, String name,
                                            Consumer<CreateCollectionRequest.Builder> configurator) {
        CreateCollectionRequest.Builder builder = CreateCollectionRequest.builder(name).getOrCreate(true);
        if (configurator != null) {
            configurator.accept(builder);
        }
        CreateCollectionRequest request = builder.build();
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections", tenant, database);
        CollectionModel model = httpClient.post(path, request, CollectionModel.class);
        return createCollectionInstance(model);
    }

    @Override
    public Collection getCollection(String tenant, String database, String collectionId) {
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections/%s",
                tenant, database, collectionId);
        CollectionModel model = httpClient.get(path, CollectionModel.class);
        return createCollectionInstance(model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Collection> listCollections(String tenant, String database) {
        return listCollections(tenant, database, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Collection> listCollections(String tenant, String database, Integer limit, Integer offset) {
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections", tenant, database);
        if (limit != null || offset != null) {
            path += buildQueryParams(limit, offset);
        }
        List<CollectionModel> models = (List<CollectionModel>) httpClient.get(path, List.class);
        List<Collection> collections = new ArrayList<>();
        if (models != null) {
            for (CollectionModel model : models) {
                collections.add(createCollectionInstance(model));
            }
        }
        return collections;
    }

    @Override
    public int countCollections(String tenant, String database) {
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections_count", tenant, database);
        return httpClient.get(path, Integer.class);
    }

    @Override
    public void deleteCollection(String tenant, String database, String collectionId) {
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections/%s",
                tenant, database, collectionId);
        httpClient.delete(path, Void.class);
    }

    @Override
    public void updateCollection(String tenant, String database, String collectionId,
                                Consumer<UpdateCollectionRequest.Builder> configurator) {
        UpdateCollectionRequest.Builder builder = UpdateCollectionRequest.builder();
        if (configurator != null) {
            configurator.accept(builder);
        }
        UpdateCollectionRequest request = builder.build();
        String path = String.format("/api/v2/tenants/%s/databases/%s/collections/%s",
                tenant, database, collectionId);
        httpClient.put(path, request, Void.class);
    }

    @Override
    public String heartbeat() {
        return httpClient.get("/api/v2/heartbeat", String.class);
    }

    @Override
    public String version() {
        return httpClient.get("/api/v2/version", String.class);
    }

    @Override
    public void reset() {
        httpClient.post("/api/v2/reset", null, Void.class);
    }

    protected String buildQueryParams(Integer limit, Integer offset) {
        StringBuilder params = new StringBuilder("?");
        if (limit != null) {
            params.append("limit=").append(limit);
        }
        if (offset != null) {
            if (limit != null) params.append("&");
            params.append("offset=").append(offset);
        }
        return params.toString();
    }

    /**
     * Create a Collection instance from a CollectionModel.
     * Subclasses can override this to create their own Collection implementations.
     */
    protected Collection createCollectionInstance(CollectionModel model) {
        return new ServerCollection(model, httpClient);
    }

    private static class CreateTenantRequest {
        private final String name;

        CreateTenantRequest(String name) {
            this.name = name;
        }
    }

    private static class CreateDatabaseRequest {
        private final String name;

        CreateDatabaseRequest(String name) {
            this.name = name;
        }
    }
}