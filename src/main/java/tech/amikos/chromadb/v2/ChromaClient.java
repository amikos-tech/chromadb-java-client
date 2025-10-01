package tech.amikos.chromadb.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simplified ChromaDB client with single approach for configuration.
 * Follows radical simplicity principles: flat package, single client, builder-only patterns.
 */
public class ChromaClient {
    private final HttpClient httpClient;
    private final String defaultTenant;
    private final String defaultDatabase;

    private ChromaClient(Builder builder) {
        this.httpClient = HttpClient.builder()
                .baseUrl(builder.baseUrl)
                .auth(builder.authProvider)
                .connectTimeout(builder.connectTimeout)
                .readTimeout(builder.readTimeout)
                .writeTimeout(builder.writeTimeout)
                .build();
        this.defaultTenant = builder.defaultTenant;
        this.defaultDatabase = builder.defaultDatabase;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Heartbeat and version
    @SuppressWarnings("unchecked")
    public String heartbeat() {
        Map<String, Object> response = httpClient.get("/api/v2/heartbeat", Map.class);
        return response.get("nanosecond heartbeat").toString();
    }

    public String version() {
        String response = httpClient.get("/api/v2/version", String.class);
        return response.replace("\"", "");
    }

    public void reset() {
        httpClient.post("/api/v2/reset", null, Void.class);
    }

    // Tenant operations
    public Tenant createTenant(String name) {
        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        return httpClient.post("/api/v2/tenants", request, Tenant.class);
    }

    public Tenant getTenant(String name) {
        return httpClient.get("/api/v2/tenants/" + name, Tenant.class);
    }

    // Database operations
    public Database createDatabase(String name) {
        return createDatabase(defaultTenant, name);
    }

    public Database createDatabase(String tenant, String name) {
        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        return httpClient.post("/api/v2/tenants/" + tenant + "/databases", request, Database.class);
    }

    public Database getDatabase(String name) {
        return getDatabase(defaultTenant, name);
    }

    public Database getDatabase(String tenant, String name) {
        return httpClient.get("/api/v2/tenants/" + tenant + "/databases/" + name, Database.class);
    }

    public List<Database> listDatabases() {
        return listDatabases(defaultTenant);
    }

    @SuppressWarnings("unchecked")
    public List<Database> listDatabases(String tenant) {
        return httpClient.get("/api/v2/tenants/" + tenant + "/databases", List.class);
    }

    public void deleteDatabase(String name) {
        deleteDatabase(defaultTenant, name);
    }

    public void deleteDatabase(String tenant, String name) {
        httpClient.delete("/api/v2/tenants/" + tenant + "/databases/" + name, Void.class);
    }

    // Collection operations - simplified overloads
    public Collection createCollection(String name) {
        return createCollection(defaultTenant, defaultDatabase, name, null);
    }

    public Collection createCollection(String name, Map<String, Object> metadata) {
        CreateCollectionRequest request = new CreateCollectionRequest.Builder(name)
                .metadata(metadata)
                .build();
        return createCollectionWithRequest(defaultTenant, defaultDatabase, request);
    }

    public Collection getCollection(String nameOrId) {
        return getCollection(defaultTenant, defaultDatabase, nameOrId);
    }

    @SuppressWarnings("unchecked")
    public Collection getCollection(String tenant, String database, String nameOrId) {
        // Try as ID first
        try {
            UUID.fromString(nameOrId);
            CollectionModel model = httpClient.get(
                "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + nameOrId,
                CollectionModel.class
            );
            return new Collection(httpClient, model);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try as name
            List<CollectionModel> collections = httpClient.get(
                "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections?name=" + nameOrId,
                List.class
            );
            if (collections.isEmpty()) {
                throw new ChromaNotFoundException("Collection not found: " + nameOrId);
            }
            return new Collection(httpClient, collections.get(0));
        }
    }

    public Collection getOrCreateCollection(String name) {
        return getOrCreateCollection(name, null);
    }

    public Collection getOrCreateCollection(String name, Map<String, Object> metadata) {
        try {
            return getCollection(name);
        } catch (ChromaNotFoundException e) {
            return createCollection(name, metadata);
        }
    }

    public List<Collection> listCollections() {
        return listCollections(defaultTenant, defaultDatabase);
    }

    @SuppressWarnings("unchecked")
    public List<Collection> listCollections(String tenant, String database) {
        List<CollectionModel> models = httpClient.get(
            "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections",
            List.class
        );
        return models.stream()
            .map(model -> new Collection(httpClient, model))
            .collect(Collectors.toList());
    }

    public void deleteCollection(String nameOrId) {
        deleteCollection(defaultTenant, defaultDatabase, nameOrId);
    }

    public void deleteCollection(String tenant, String database, String nameOrId) {
        Collection collection = getCollection(tenant, database, nameOrId);
        httpClient.delete("/api/v2/tenants/" + tenant + "/databases/" + database +
                          "/collections/" + collection.getId(), Void.class);
    }

    public int countCollections() {
        return countCollections(defaultTenant, defaultDatabase);
    }

    public int countCollections(String tenant, String database) {
        Integer count = httpClient.get(
            "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections_count",
            Integer.class
        );
        return count;
    }

    // Private helpers
    private Collection createCollectionWithRequest(String tenant, String database, CreateCollectionRequest request) {
        CollectionModel model = httpClient.post(
            "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections",
            request,
            CollectionModel.class
        );
        return new Collection(httpClient, model);
    }

    private Collection createCollection(String tenant, String database, String name, Map<String, Object> metadata) {
        CreateCollectionRequest request = new CreateCollectionRequest.Builder(name)
                .metadata(metadata)
                .build();
        return createCollectionWithRequest(tenant, database, request);
    }

    public static class Builder {
        private String baseUrl = "http://localhost:8000";
        private AuthProvider authProvider = NoAuthProvider.INSTANCE;
        private String defaultTenant = "default_tenant";
        private String defaultDatabase = "default_database";
        private int connectTimeout = 60;
        private int readTimeout = 60;
        private int writeTimeout = 60;

        // Server mode configuration
        public Builder serverUrl(String url) {
            this.baseUrl = url;
            return this;
        }

        // Cloud mode configuration (syntactic sugar)
        public Builder cloudUrl(String url) {
            this.baseUrl = url;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.authProvider = new TokenAuthProvider(apiKey);
            return this;
        }

        public Builder auth(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder tenant(String tenant) {
            this.defaultTenant = tenant;
            return this;
        }

        public Builder database(String database) {
            this.defaultDatabase = database;
            return this;
        }

        public Builder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }

        public Builder readTimeout(int seconds) {
            this.readTimeout = seconds;
            return this;
        }

        public Builder writeTimeout(int seconds) {
            this.writeTimeout = seconds;
            return this;
        }

        public ChromaClient build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL is required");
            }
            return new ChromaClient(this);
        }
    }
}