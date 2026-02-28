package tech.amikos.chromadb.v2;

import java.time.Duration;
import java.util.Map;

/**
 * Entry point for creating ChromaDB clients.
 *
 * <pre>{@code
 * // Self-hosted
 * Client client = ChromaClient.builder()
 *     .baseUrl("http://localhost:8000")
 *     .build();
 *
 * // Chroma Cloud
 * Client client = ChromaClient.cloud()
 *     .apiKey("your-key")
 *     .tenant("my-tenant")
 *     .database("my-db")
 *     .build();
 * }</pre>
 */
public final class ChromaClient {

    private ChromaClient() {}

    public static Builder builder() {
        return new Builder();
    }

    public static CloudBuilder cloud() {
        return new CloudBuilder();
    }

    public static final class Builder {
        private String baseUrl;
        private AuthProvider authProvider;
        private String apiKey;
        private Tenant tenant;
        private Database database;
        private Duration connectTimeout;
        private Duration readTimeout;
        private Duration writeTimeout;
        private Map<String, String> defaultHeaders;

        Builder() {}

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        public Builder auth(AuthProvider authProvider) { this.authProvider = authProvider; return this; }

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }

        public Builder tenant(Tenant tenant) { this.tenant = tenant; return this; }

        public Builder tenant(String tenant) { this.tenant = Tenant.of(tenant); return this; }

        public Builder database(Database database) { this.database = database; return this; }

        public Builder database(String database) { this.database = Database.of(database); return this; }

        public Builder timeout(Duration timeout) {
            this.connectTimeout = timeout;
            this.readTimeout = timeout;
            this.writeTimeout = timeout;
            return this;
        }

        public Builder connectTimeout(Duration timeout) { this.connectTimeout = timeout; return this; }

        public Builder readTimeout(Duration timeout) { this.readTimeout = timeout; return this; }

        public Builder writeTimeout(Duration timeout) { this.writeTimeout = timeout; return this; }

        public Builder defaultHeaders(Map<String, String> headers) { this.defaultHeaders = headers; return this; }

        public Client build() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public static final class CloudBuilder {
        private String apiKey;
        private String tenant;
        private String database;
        private Duration timeout;

        CloudBuilder() {}

        public CloudBuilder apiKey(String apiKey) { this.apiKey = apiKey; return this; }

        public CloudBuilder tenant(String tenant) { this.tenant = tenant; return this; }

        public CloudBuilder database(String database) { this.database = database; return this; }

        public CloudBuilder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public Client build() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
