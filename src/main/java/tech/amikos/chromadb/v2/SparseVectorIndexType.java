package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for sparse vector index. */
public final class SparseVectorIndexType {
    private final boolean enabled;
    private final SparseVectorIndexConfig config;

    private SparseVectorIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public SparseVectorIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseVectorIndexType)) return false;
        SparseVectorIndexType that = (SparseVectorIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "SparseVectorIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private SparseVectorIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(SparseVectorIndexConfig config) { this.config = config; return this; }
        public SparseVectorIndexType build() { return new SparseVectorIndexType(this); }
    }
}
