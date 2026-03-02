package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for vector index. */
public final class VectorIndexType {
    private final boolean enabled;
    private final VectorIndexConfig config;

    private VectorIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public VectorIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorIndexType)) return false;
        VectorIndexType that = (VectorIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "VectorIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private VectorIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(VectorIndexConfig config) { this.config = config; return this; }
        public VectorIndexType build() { return new VectorIndexType(this); }
    }
}
