package tech.amikos.chromadb.v2;

/** Marker type for an empty float-inverted-index config payload. */
public final class FloatInvertedIndexConfig {
    @Override
    public boolean equals(Object obj) { return obj instanceof FloatInvertedIndexConfig; }
    @Override
    public int hashCode() { return FloatInvertedIndexConfig.class.hashCode(); }
    @Override
    public String toString() { return "FloatInvertedIndexConfig{}"; }
}
