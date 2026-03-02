package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for string values. */
public final class StringValueType {
    private final FtsIndexType ftsIndex;
    private final StringInvertedIndexType stringInvertedIndex;

    private StringValueType(Builder builder) {
        this.ftsIndex = builder.ftsIndex;
        this.stringInvertedIndex = builder.stringInvertedIndex;
    }

    public static Builder builder() { return new Builder(); }
    public FtsIndexType getFtsIndex() { return ftsIndex; }
    public StringInvertedIndexType getStringInvertedIndex() { return stringInvertedIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringValueType)) return false;
        StringValueType that = (StringValueType) o;
        return Objects.equals(ftsIndex, that.ftsIndex)
                && Objects.equals(stringInvertedIndex, that.stringInvertedIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(ftsIndex, stringInvertedIndex); }

    @Override
    public String toString() { return "StringValueType{" + "ftsIndex=" + ftsIndex + ", stringInvertedIndex=" + stringInvertedIndex + '}'; }

    public static final class Builder {
        private FtsIndexType ftsIndex;
        private StringInvertedIndexType stringInvertedIndex;

        Builder() {}
        public Builder ftsIndex(FtsIndexType ftsIndex) { this.ftsIndex = ftsIndex; return this; }
        public Builder stringInvertedIndex(StringInvertedIndexType stringInvertedIndex) { this.stringInvertedIndex = stringInvertedIndex; return this; }
        public StringValueType build() { return new StringValueType(this); }
    }
}
