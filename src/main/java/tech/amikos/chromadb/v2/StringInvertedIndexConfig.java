package tech.amikos.chromadb.v2;

/** Marker type for an empty string-inverted-index config payload. */
public final class StringInvertedIndexConfig {
    @Override
    public boolean equals(Object obj) { return obj instanceof StringInvertedIndexConfig; }
    @Override
    public int hashCode() { return StringInvertedIndexConfig.class.hashCode(); }
    @Override
    public String toString() { return "StringInvertedIndexConfig{}"; }
}
