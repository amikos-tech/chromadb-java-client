package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.auth.AuthProvider;
import tech.amikos.chromadb.v2.http.HttpClient;

import java.util.function.Consumer;

public class ServerClient extends BaseClient {
    private final String defaultTenant;
    private final String defaultDatabase;

    private ServerClient(Builder builder) {
        super(HttpClient.builder()
                .baseUrl(builder.baseUrl)
                .auth(builder.authProvider)
                .connectTimeout(builder.connectTimeout)
                .readTimeout(builder.readTimeout)
                .writeTimeout(builder.writeTimeout)
                .build());
        this.defaultTenant = builder.defaultTenant;
        this.defaultDatabase = builder.defaultDatabase;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection createCollection(String name) {
        return createCollection(defaultTenant, defaultDatabase, name, null);
    }

    public Collection createCollection(String name, Consumer<tech.amikos.chromadb.v2.model.CreateCollectionRequest.Builder> configurator) {
        return createCollection(defaultTenant, defaultDatabase, name, configurator);
    }

    public Collection getOrCreateCollection(String name) {
        return getOrCreateCollection(defaultTenant, defaultDatabase, name, null);
    }

    public Collection getOrCreateCollection(String name, Consumer<tech.amikos.chromadb.v2.model.CreateCollectionRequest.Builder> configurator) {
        return getOrCreateCollection(defaultTenant, defaultDatabase, name, configurator);
    }

    public Collection getCollection(String collectionId) {
        return getCollection(defaultTenant, defaultDatabase, collectionId);
    }

    public void deleteCollection(String collectionId) {
        deleteCollection(defaultTenant, defaultDatabase, collectionId);
    }

    public java.util.List<Collection> listCollections() {
        return listCollections(defaultTenant, defaultDatabase);
    }

    public int countCollections() {
        return countCollections(defaultTenant, defaultDatabase);
    }

    public static class Builder {
        private String baseUrl;
        private AuthProvider authProvider = AuthProvider.none();
        private String defaultTenant = "default";
        private String defaultDatabase = "default";
        private int connectTimeout = 60;
        private int readTimeout = 60;
        private int writeTimeout = 60;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder auth(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder defaultTenant(String tenant) {
            this.defaultTenant = tenant;
            return this;
        }

        public Builder defaultDatabase(String database) {
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

        public ServerClient build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            return new ServerClient(this);
        }
    }
}