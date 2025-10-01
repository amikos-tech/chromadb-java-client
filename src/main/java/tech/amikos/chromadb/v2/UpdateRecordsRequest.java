package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateRecordsRequest {
    @SerializedName("ids")
    private final List<String> ids;

    @SerializedName("embeddings")
    private final Object embeddings;

    @SerializedName("documents")
    private final List<String> documents;

    @SerializedName("metadatas")
    private final List<Map<String, Object>> metadatas;

    @SerializedName("uris")
    private final List<String> uris;

    private UpdateRecordsRequest(Builder builder) {
        this.ids = builder.ids;
        this.embeddings = builder.embeddings;
        this.documents = builder.documents;
        this.metadatas = builder.metadatas;
        this.uris = builder.uris;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> ids = new ArrayList<>();
        private Object embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<String> uris;

        public Builder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        public Builder embeddings(List<List<Float>> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        public Builder metadatas(List<Map<String, Object>> metadatas) {
            this.metadatas = metadatas;
            return this;
        }

        public Builder uris(List<String> uris) {
            this.uris = uris;
            return this;
        }

        public UpdateRecordsRequest build() {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids are required");
            }
            return new UpdateRecordsRequest(this);
        }
    }
}