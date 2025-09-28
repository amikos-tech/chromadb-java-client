package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.auth.AuthProvider;
import tech.amikos.chromadb.v2.http.HttpClient;

public class CloudClient extends BaseClient {

    private CloudClient(Builder builder) {
        super(HttpClient.builder()
                .baseUrl(builder.cloudUrl)
                .auth(AuthProvider.token(builder.apiKey))
                .connectTimeout(builder.connectTimeout)
                .readTimeout(builder.readTimeout)
                .writeTimeout(builder.writeTimeout)
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String cloudUrl = "https://api.trychroma.com";
        private String region;
        private int connectTimeout = 60;
        private int readTimeout = 60;
        private int writeTimeout = 60;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder cloudUrl(String cloudUrl) {
            this.cloudUrl = cloudUrl;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
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

        public CloudClient build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey is required for CloudClient");
            }
            return new CloudClient(this);
        }
    }
}