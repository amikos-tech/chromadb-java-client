package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for string inverted index. */
public final class StringInvertedIndexType {
    private final boolean enabled;
    private final StringInvertedIndexConfig config;

    private StringInvertedIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public StringInvertedIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringInvertedIndexType)) return false;
        StringInvertedIndexType that = (StringInvertedIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "StringInvertedIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private StringInvertedIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(StringInvertedIndexConfig config) { this.config = config; return this; }
        public StringInvertedIndexType build() { return new StringInvertedIndexType(this); }
    }
}
