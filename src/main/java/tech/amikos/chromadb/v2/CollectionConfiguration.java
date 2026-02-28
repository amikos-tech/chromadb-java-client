package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable collection configuration (HNSW parameters). */
public final class CollectionConfiguration {

    private final DistanceFunction space;
    private final Integer hnswM;
    private final Integer hnswConstructionEf;
    private final Integer hnswSearchEf;
    private final Integer hnswBatchSize;
    private final Integer hnswSyncThreshold;

    private CollectionConfiguration(Builder builder) {
        this.space = builder.space;
        this.hnswM = builder.hnswM;
        this.hnswConstructionEf = builder.hnswConstructionEf;
        this.hnswSearchEf = builder.hnswSearchEf;
        this.hnswBatchSize = builder.hnswBatchSize;
        this.hnswSyncThreshold = builder.hnswSyncThreshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public DistanceFunction getSpace() { return space; }
    public Integer getHnswM() { return hnswM; }
    public Integer getHnswConstructionEf() { return hnswConstructionEf; }
    public Integer getHnswSearchEf() { return hnswSearchEf; }
    public Integer getHnswBatchSize() { return hnswBatchSize; }
    public Integer getHnswSyncThreshold() { return hnswSyncThreshold; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionConfiguration)) return false;
        CollectionConfiguration that = (CollectionConfiguration) o;
        return space == that.space
                && Objects.equals(hnswM, that.hnswM)
                && Objects.equals(hnswConstructionEf, that.hnswConstructionEf)
                && Objects.equals(hnswSearchEf, that.hnswSearchEf)
                && Objects.equals(hnswBatchSize, that.hnswBatchSize)
                && Objects.equals(hnswSyncThreshold, that.hnswSyncThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, hnswM, hnswConstructionEf, hnswSearchEf, hnswBatchSize, hnswSyncThreshold);
    }

    @Override
    public String toString() {
        return "CollectionConfiguration{"
                + "space=" + space
                + ", hnswM=" + hnswM
                + ", hnswConstructionEf=" + hnswConstructionEf
                + ", hnswSearchEf=" + hnswSearchEf
                + ", hnswBatchSize=" + hnswBatchSize
                + ", hnswSyncThreshold=" + hnswSyncThreshold
                + '}';
    }

    public static final class Builder {
        private DistanceFunction space;
        private Integer hnswM;
        private Integer hnswConstructionEf;
        private Integer hnswSearchEf;
        private Integer hnswBatchSize;
        private Integer hnswSyncThreshold;

        Builder() {}

        public Builder space(DistanceFunction space) { this.space = space; return this; }
        /** @throws IllegalArgumentException if {@code m <= 0} */
        public Builder hnswM(int m) { this.hnswM = requirePositive("hnswM", m); return this; }
        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder hnswConstructionEf(int ef) { this.hnswConstructionEf = requirePositive("hnswConstructionEf", ef); return this; }
        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder hnswSearchEf(int ef) { this.hnswSearchEf = requirePositive("hnswSearchEf", ef); return this; }
        /** @throws IllegalArgumentException if {@code size <= 0} */
        public Builder hnswBatchSize(int size) { this.hnswBatchSize = requirePositive("hnswBatchSize", size); return this; }
        /** @throws IllegalArgumentException if {@code threshold <= 0} */
        public Builder hnswSyncThreshold(int threshold) { this.hnswSyncThreshold = requirePositive("hnswSyncThreshold", threshold); return this; }

        public CollectionConfiguration build() {
            return new CollectionConfiguration(this);
        }

        private static int requirePositive(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0");
            }
            return value;
        }
    }
}
