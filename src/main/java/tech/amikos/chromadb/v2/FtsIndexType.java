package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Enabled/config wrapper for full-text index. */
public final class FtsIndexType {
    private final boolean enabled;
    private final FtsIndexConfig config;

    private FtsIndexType(Builder builder) {
        this.enabled = builder.enabled;
        this.config = builder.config;
    }

    public static Builder builder() { return new Builder(); }
    public boolean isEnabled() { return enabled; }
    public FtsIndexConfig getConfig() { return config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FtsIndexType)) return false;
        FtsIndexType that = (FtsIndexType) o;
        return enabled == that.enabled && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() { return Objects.hash(Boolean.valueOf(enabled), config); }

    @Override
    public String toString() { return "FtsIndexType{" + "enabled=" + enabled + ", config=" + config + '}'; }

    public static final class Builder {
        private boolean enabled = true;
        private FtsIndexConfig config;

        Builder() {}
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(FtsIndexConfig config) { this.config = config; return this; }
        public FtsIndexType build() { return new FtsIndexType(this); }
    }
}
