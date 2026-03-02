package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for int inverted index. */
public final class IntInvertedIndexType {
    private final boolean enabled;
    private final IntInvertedIndexConfig config;

    private IntInvertedIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public IntInvertedIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntInvertedIndexType)) return false;
        IntInvertedIndexType that = (IntInvertedIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "IntInvertedIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private IntInvertedIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(IntInvertedIndexConfig config) { this.config = config; return this; }
        public IntInvertedIndexType build() { return new IntInvertedIndexType(this); }
    }
}
