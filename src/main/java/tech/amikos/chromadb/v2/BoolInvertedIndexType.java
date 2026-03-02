package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for bool inverted index. */
public final class BoolInvertedIndexType {
    private final boolean enabled;
    private final BoolInvertedIndexConfig config;

    private BoolInvertedIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public BoolInvertedIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoolInvertedIndexType)) return false;
        BoolInvertedIndexType that = (BoolInvertedIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "BoolInvertedIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private BoolInvertedIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(BoolInvertedIndexConfig config) { this.config = config; return this; }
        public BoolInvertedIndexType build() { return new BoolInvertedIndexType(this); }
    }
}
