package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for float inverted index. */
public final class FloatInvertedIndexType {
    private final boolean enabled;
    private final FloatInvertedIndexConfig config;

    private FloatInvertedIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public FloatInvertedIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatInvertedIndexType)) return false;
        FloatInvertedIndexType that = (FloatInvertedIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "FloatInvertedIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private FloatInvertedIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(FloatInvertedIndexConfig config) { this.config = config; return this; }
        public FloatInvertedIndexType build() { return new FloatInvertedIndexType(this); }
    }
}
