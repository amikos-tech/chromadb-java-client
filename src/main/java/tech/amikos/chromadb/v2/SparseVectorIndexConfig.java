package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Sparse vector index config. */
public final class SparseVectorIndexConfig {

    private final String sourceKey;
    private final Boolean bm25;
    private final EmbeddingFunctionSpec embeddingFunction;

    private SparseVectorIndexConfig(Builder builder) {
        this.sourceKey = normalizeNullable(builder.sourceKey);
        this.bm25 = builder.bm25;
        this.embeddingFunction = builder.embeddingFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public Boolean getBm25() {
        return bm25;
    }

    public EmbeddingFunctionSpec getEmbeddingFunction() {
        return embeddingFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseVectorIndexConfig)) return false;
        SparseVectorIndexConfig that = (SparseVectorIndexConfig) o;
        return Objects.equals(sourceKey, that.sourceKey)
                && Objects.equals(bm25, that.bm25)
                && Objects.equals(embeddingFunction, that.embeddingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceKey, bm25, embeddingFunction);
    }

    @Override
    public String toString() {
        return "SparseVectorIndexConfig{"
                + "sourceKey='" + sourceKey + '\''
                + ", bm25=" + bm25
                + ", embeddingFunction=" + embeddingFunction
                + '}';
    }

    public static final class Builder {
        private String sourceKey;
        private Boolean bm25;
        private EmbeddingFunctionSpec embeddingFunction;

        Builder() {}

        public Builder sourceKey(String sourceKey) { this.sourceKey = sourceKey; return this; }
        public Builder bm25(boolean bm25) { this.bm25 = Boolean.valueOf(bm25); return this; }
        public Builder embeddingFunction(EmbeddingFunctionSpec embeddingFunction) { this.embeddingFunction = embeddingFunction; return this; }

        public SparseVectorIndexConfig build() {
            return new SparseVectorIndexConfig(this);
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
