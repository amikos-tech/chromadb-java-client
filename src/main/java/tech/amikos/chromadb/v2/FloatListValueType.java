package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for dense vectors (float lists). */
public final class FloatListValueType {
    private final VectorIndexType vectorIndex;

    private FloatListValueType(Builder builder) {
        this.vectorIndex = builder.vectorIndex;
    }

    public static Builder builder() { return new Builder(); }
    public VectorIndexType getVectorIndex() { return vectorIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatListValueType)) return false;
        FloatListValueType that = (FloatListValueType) o;
        return Objects.equals(vectorIndex, that.vectorIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(vectorIndex); }

    @Override
    public String toString() { return "FloatListValueType{" + "vectorIndex=" + vectorIndex + '}'; }

    public static final class Builder {
        private VectorIndexType vectorIndex;

        Builder() {}
        public Builder vectorIndex(VectorIndexType vectorIndex) { this.vectorIndex = vectorIndex; return this; }
        public FloatListValueType build() { return new FloatListValueType(this); }
    }
}
