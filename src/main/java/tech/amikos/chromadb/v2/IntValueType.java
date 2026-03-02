package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for ints. */
public final class IntValueType {
    private final IntInvertedIndexType intInvertedIndex;

    private IntValueType(Builder builder) {
        this.intInvertedIndex = builder.intInvertedIndex;
    }

    public static Builder builder() { return new Builder(); }
    public IntInvertedIndexType getIntInvertedIndex() { return intInvertedIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntValueType)) return false;
        IntValueType that = (IntValueType) o;
        return Objects.equals(intInvertedIndex, that.intInvertedIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(intInvertedIndex); }

    @Override
    public String toString() { return "IntValueType{" + "intInvertedIndex=" + intInvertedIndex + '}'; }

    public static final class Builder {
        private IntInvertedIndexType intInvertedIndex;

        Builder() {}
        public Builder intInvertedIndex(IntInvertedIndexType intInvertedIndex) { this.intInvertedIndex = intInvertedIndex; return this; }
        public IntValueType build() { return new IntValueType(this); }
    }
}
