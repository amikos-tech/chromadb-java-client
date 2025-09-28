package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

public class CollectionConfiguration {
    @SerializedName("embedding_function")
    private final EmbeddingFunctionConfig embeddingFunction;

    @SerializedName("hnsw")
    private final HnswConfiguration hnsw;

    @SerializedName("spann")
    private final SpannConfiguration spann;

    private CollectionConfiguration(Builder builder) {
        this.embeddingFunction = builder.embeddingFunction;
        this.hnsw = builder.hnsw;
        this.spann = builder.spann;
    }

    public EmbeddingFunctionConfig getEmbeddingFunction() {
        return embeddingFunction;
    }

    public HnswConfiguration getHnsw() {
        return hnsw;
    }

    public SpannConfiguration getSpann() {
        return spann;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EmbeddingFunctionConfig embeddingFunction;
        private HnswConfiguration hnsw;
        private SpannConfiguration spann;

        public Builder embeddingFunction(EmbeddingFunctionConfig embeddingFunction) {
            this.embeddingFunction = embeddingFunction;
            return this;
        }

        public Builder hnsw(HnswConfiguration hnsw) {
            this.hnsw = hnsw;
            return this;
        }

        public Builder spann(SpannConfiguration spann) {
            this.spann = spann;
            return this;
        }

        public CollectionConfiguration build() {
            return new CollectionConfiguration(this);
        }
    }
}