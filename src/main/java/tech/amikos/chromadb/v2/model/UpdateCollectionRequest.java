package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class UpdateCollectionRequest {
    @SerializedName("name")
    private final String name;

    @SerializedName("metadata")
    private final Map<String, Object> metadata;

    @SerializedName("configuration")
    private final CollectionConfiguration configuration;

    private UpdateCollectionRequest(Builder builder) {
        this.name = builder.name;
        this.metadata = builder.metadata;
        this.configuration = builder.configuration;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private Map<String, Object> metadata;
        private CollectionConfiguration configuration;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder configuration(CollectionConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public UpdateCollectionRequest build() {
            return new UpdateCollectionRequest(this);
        }
    }
}