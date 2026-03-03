package tech.amikos.chromadb.v2;

/** Marker type for an empty bool-inverted-index config payload. */
public final class BoolInvertedIndexConfig {
    @Override
    public boolean equals(Object obj) { return obj instanceof BoolInvertedIndexConfig; }
    @Override
    public int hashCode() { return BoolInvertedIndexConfig.class.hashCode(); }
    @Override
    public String toString() { return "BoolInvertedIndexConfig{}"; }
}
