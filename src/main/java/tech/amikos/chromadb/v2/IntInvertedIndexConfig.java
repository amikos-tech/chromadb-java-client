package tech.amikos.chromadb.v2;

/** Marker type for an empty int-inverted-index config payload. */
public final class IntInvertedIndexConfig {
    @Override
    public boolean equals(Object obj) { return obj instanceof IntInvertedIndexConfig; }
    @Override
    public int hashCode() { return IntInvertedIndexConfig.class.hashCode(); }
    @Override
    public String toString() { return "IntInvertedIndexConfig{}"; }
}
