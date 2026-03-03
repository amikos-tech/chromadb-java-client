package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for floats. */
public final class FloatValueType {
    private final FloatInvertedIndexType floatInvertedIndex;

    private FloatValueType(Builder builder) {
        this.floatInvertedIndex = builder.floatInvertedIndex;
    }

    public static Builder builder() { return new Builder(); }
    public FloatInvertedIndexType getFloatInvertedIndex() { return floatInvertedIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatValueType)) return false;
        FloatValueType that = (FloatValueType) o;
        return Objects.equals(floatInvertedIndex, that.floatInvertedIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(floatInvertedIndex); }

    @Override
    public String toString() { return "FloatValueType{" + "floatInvertedIndex=" + floatInvertedIndex + '}'; }

    public static final class Builder {
        private FloatInvertedIndexType floatInvertedIndex;

        Builder() {}
        public Builder floatInvertedIndex(FloatInvertedIndexType floatInvertedIndex) { this.floatInvertedIndex = floatInvertedIndex; return this; }
        public FloatValueType build() { return new FloatValueType(this); }
    }
}
