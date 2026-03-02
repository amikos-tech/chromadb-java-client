package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for booleans. */
public final class BoolValueType {
    private final BoolInvertedIndexType boolInvertedIndex;

    private BoolValueType(Builder builder) {
        this.boolInvertedIndex = builder.boolInvertedIndex;
    }

    public static Builder builder() { return new Builder(); }
    public BoolInvertedIndexType getBoolInvertedIndex() { return boolInvertedIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoolValueType)) return false;
        BoolValueType that = (BoolValueType) o;
        return Objects.equals(boolInvertedIndex, that.boolInvertedIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(boolInvertedIndex); }

    @Override
    public String toString() { return "BoolValueType{" + "boolInvertedIndex=" + boolInvertedIndex + '}'; }

    public static final class Builder {
        private BoolInvertedIndexType boolInvertedIndex;

        Builder() {}
        public Builder boolInvertedIndex(BoolInvertedIndexType boolInvertedIndex) { this.boolInvertedIndex = boolInvertedIndex; return this; }
        public BoolValueType build() { return new BoolValueType(this); }
    }
}
