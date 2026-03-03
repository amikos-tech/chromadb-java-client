package tech.amikos.chromadb.v2;

/** Marker type for an empty full-text index config payload. */
public final class FtsIndexConfig {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FtsIndexConfig;
    }

    @Override
    public int hashCode() {
        return FtsIndexConfig.class.hashCode();
    }

    @Override
    public String toString() {
        return "FtsIndexConfig{}";
    }
}
