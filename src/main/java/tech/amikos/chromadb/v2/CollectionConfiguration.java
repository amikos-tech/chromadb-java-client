package tech.amikos.chromadb.v2;

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

    public static final class Builder {
        private DistanceFunction space;
        private Integer hnswM;
        private Integer hnswConstructionEf;
        private Integer hnswSearchEf;
        private Integer hnswBatchSize;
        private Integer hnswSyncThreshold;

        Builder() {}

        public Builder space(DistanceFunction space) { this.space = space; return this; }
        public Builder hnswM(int m) { this.hnswM = m; return this; }
        public Builder hnswConstructionEf(int ef) { this.hnswConstructionEf = ef; return this; }
        public Builder hnswSearchEf(int ef) { this.hnswSearchEf = ef; return this; }
        public Builder hnswBatchSize(int size) { this.hnswBatchSize = size; return this; }
        public Builder hnswSyncThreshold(int threshold) { this.hnswSyncThreshold = threshold; return this; }

        public CollectionConfiguration build() {
            return new CollectionConfiguration(this);
        }
    }
}
