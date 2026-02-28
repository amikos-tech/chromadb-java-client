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
        Builder() {}

        public Builder baseUrl(String baseUrl) { return this; }

        public Builder auth(AuthProvider authProvider) { return this; }

        public Builder apiKey(String apiKey) { return this; }

        public Builder tenant(Tenant tenant) { return this; }

        public Builder tenant(String tenant) { return this; }

        public Builder database(Database database) { return this; }

        public Builder database(String database) { return this; }

        public Builder timeout(Duration timeout) { return this; }

        public Builder connectTimeout(Duration timeout) { return this; }

        public Builder readTimeout(Duration timeout) { return this; }

        public Builder writeTimeout(Duration timeout) { return this; }

        public Builder defaultHeaders(Map<String, String> headers) { return this; }

        public Client build() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public static final class CloudBuilder {
        CloudBuilder() {}

        public CloudBuilder apiKey(String apiKey) { return this; }

        public CloudBuilder tenant(String tenant) { return this; }

        public CloudBuilder database(String database) { return this; }

        public CloudBuilder timeout(Duration timeout) { return this; }

        public Client build() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
