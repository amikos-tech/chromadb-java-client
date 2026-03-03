package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Value-type index configuration for sparse vectors. */
public final class SparseVectorValueType {
    private final SparseVectorIndexType sparseVectorIndex;

    private SparseVectorValueType(Builder builder) {
        this.sparseVectorIndex = builder.sparseVectorIndex;
    }

    public static Builder builder() { return new Builder(); }
    public SparseVectorIndexType getSparseVectorIndex() { return sparseVectorIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseVectorValueType)) return false;
        SparseVectorValueType that = (SparseVectorValueType) o;
        return Objects.equals(sparseVectorIndex, that.sparseVectorIndex);
    }

    @Override
    public int hashCode() { return Objects.hash(sparseVectorIndex); }

    @Override
    public String toString() { return "SparseVectorValueType{" + "sparseVectorIndex=" + sparseVectorIndex + '}'; }

    public static final class Builder {
        private SparseVectorIndexType sparseVectorIndex;

        Builder() {}
        public Builder sparseVectorIndex(SparseVectorIndexType sparseVectorIndex) { this.sparseVectorIndex = sparseVectorIndex; return this; }
        public SparseVectorValueType build() { return new SparseVectorValueType(this); }
    }
}
