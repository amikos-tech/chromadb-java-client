package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class CreateCollectionRequest {
    @SerializedName("name")
    private final String name;

    @SerializedName("metadata")
    private final Map<String, Object> metadata;

    @SerializedName("configuration")
    private final CollectionConfiguration configuration;

    @SerializedName("get_or_create")
    private final Boolean getOrCreate;

    private CreateCollectionRequest(Builder builder) {
        this.name = builder.name;
        this.metadata = builder.metadata;
        this.configuration = builder.configuration;
        this.getOrCreate = builder.getOrCreate;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private Map<String, Object> metadata;
        private CollectionConfiguration configuration;
        private Boolean getOrCreate = false;

        public Builder(String name) {
            this.name = name;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder configuration(CollectionConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder getOrCreate(boolean getOrCreate) {
            this.getOrCreate = getOrCreate;
            return this;
        }

        public CreateCollectionRequest build() {
            return new CreateCollectionRequest(this);
        }
    }
}