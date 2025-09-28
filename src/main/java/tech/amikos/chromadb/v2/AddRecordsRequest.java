package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddRecordsRequest {
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

    private AddRecordsRequest(Builder builder) {
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

        public Builder id(String id) {
            this.ids.add(id);
            return this;
        }

        public Builder embeddings(List<List<Float>> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder embeddingsAsBase64(List<String> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        public Builder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        public Builder document(String document) {
            if (this.documents == null) {
                this.documents = new ArrayList<>();
            }
            this.documents.add(document);
            return this;
        }

        public Builder metadatas(List<Map<String, Object>> metadatas) {
            this.metadatas = metadatas;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (this.metadatas == null) {
                this.metadatas = new ArrayList<>();
            }
            this.metadatas.add(metadata);
            return this;
        }

        public Builder uris(List<String> uris) {
            this.uris = uris;
            return this;
        }

        public Builder uri(String uri) {
            if (this.uris == null) {
                this.uris = new ArrayList<>();
            }
            this.uris.add(uri);
            return this;
        }

        public AddRecordsRequest build() {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids are required");
            }
            if (embeddings == null) {
                throw new IllegalArgumentException("embeddings are required");
            }
            return new AddRecordsRequest(this);
        }
    }
}