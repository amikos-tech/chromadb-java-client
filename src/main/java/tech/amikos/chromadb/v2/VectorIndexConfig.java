package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Vector index schema config. */
public final class VectorIndexConfig {

    private final DistanceFunction space;
    private final String sourceKey;
    private final HnswIndexConfig hnsw;
    private final SpannIndexConfig spann;
    private final EmbeddingFunctionSpec embeddingFunction;

    private VectorIndexConfig(Builder builder) {
        this.space = builder.space;
        this.sourceKey = normalizeNullable(builder.sourceKey);
        this.hnsw = builder.hnsw;
        this.spann = builder.spann;
        this.embeddingFunction = builder.embeddingFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public DistanceFunction getSpace() { return space; }
    public String getSourceKey() { return sourceKey; }
    public HnswIndexConfig getHnsw() { return hnsw; }
    public SpannIndexConfig getSpann() { return spann; }
    public EmbeddingFunctionSpec getEmbeddingFunction() { return embeddingFunction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorIndexConfig)) return false;
        VectorIndexConfig that = (VectorIndexConfig) o;
        return space == that.space
                && Objects.equals(sourceKey, that.sourceKey)
                && Objects.equals(hnsw, that.hnsw)
                && Objects.equals(spann, that.spann)
                && Objects.equals(embeddingFunction, that.embeddingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, sourceKey, hnsw, spann, embeddingFunction);
    }

    @Override
    public String toString() {
        return "VectorIndexConfig{"
                + "space=" + space
                + ", sourceKey='" + sourceKey + '\''
                + ", hnsw=" + hnsw
                + ", spann=" + spann
                + ", embeddingFunction=" + embeddingFunction
                + '}';
    }

    public static final class Builder {
        private DistanceFunction space;
        private String sourceKey;
        private HnswIndexConfig hnsw;
        private SpannIndexConfig spann;
        private EmbeddingFunctionSpec embeddingFunction;

        Builder() {}

        public Builder space(DistanceFunction space) { this.space = space; return this; }
        public Builder sourceKey(String sourceKey) { this.sourceKey = sourceKey; return this; }
        public Builder hnsw(HnswIndexConfig hnsw) { this.hnsw = hnsw; return this; }
        public Builder spann(SpannIndexConfig spann) { this.spann = spann; return this; }
        public Builder embeddingFunction(EmbeddingFunctionSpec embeddingFunction) { this.embeddingFunction = embeddingFunction; return this; }

        public VectorIndexConfig build() {
            if (hnsw != null && spann != null) {
                throw new IllegalStateException("VectorIndexConfig cannot define both hnsw and spann");
            }
            return new VectorIndexConfig(this);
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
