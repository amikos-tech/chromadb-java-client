package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable collection configuration (flat HNSW/SPANN params + optional schema/embedding descriptor). */
public final class CollectionConfiguration {

    private final DistanceFunction space;
    private final Integer hnswM;
    private final Integer hnswConstructionEf;
    private final Integer hnswSearchEf;
    private final Integer hnswNumThreads;
    private final Integer hnswBatchSize;
    private final Integer hnswSyncThreshold;
    private final Double hnswResizeFactor;
    private final Integer spannSearchNprobe;
    private final Integer spannEfSearch;
    private final Schema schema;
    private final EmbeddingFunctionSpec embeddingFunction;

    private CollectionConfiguration(Builder builder) {
        this.space = builder.space;
        this.hnswM = builder.hnswM;
        this.hnswConstructionEf = builder.hnswConstructionEf;
        this.hnswSearchEf = builder.hnswSearchEf;
        this.hnswNumThreads = builder.hnswNumThreads;
        this.hnswBatchSize = builder.hnswBatchSize;
        this.hnswSyncThreshold = builder.hnswSyncThreshold;
        this.hnswResizeFactor = builder.hnswResizeFactor;
        this.spannSearchNprobe = builder.spannSearchNprobe;
        this.spannEfSearch = builder.spannEfSearch;
        this.schema = builder.schema;
        this.embeddingFunction = builder.embeddingFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public DistanceFunction getSpace() { return space; }
    public Integer getHnswM() { return hnswM; }
    public Integer getHnswConstructionEf() { return hnswConstructionEf; }
    public Integer getHnswSearchEf() { return hnswSearchEf; }
    public Integer getHnswNumThreads() { return hnswNumThreads; }
    public Integer getHnswBatchSize() { return hnswBatchSize; }
    public Integer getHnswSyncThreshold() { return hnswSyncThreshold; }
    public Double getHnswResizeFactor() { return hnswResizeFactor; }
    public Integer getSpannSearchNprobe() { return spannSearchNprobe; }
    public Integer getSpannEfSearch() { return spannEfSearch; }
    public Schema getSchema() { return schema; }
    public EmbeddingFunctionSpec getEmbeddingFunction() { return embeddingFunction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionConfiguration)) return false;
        CollectionConfiguration that = (CollectionConfiguration) o;
        return space == that.space
                && Objects.equals(hnswM, that.hnswM)
                && Objects.equals(hnswConstructionEf, that.hnswConstructionEf)
                && Objects.equals(hnswSearchEf, that.hnswSearchEf)
                && Objects.equals(hnswNumThreads, that.hnswNumThreads)
                && Objects.equals(hnswBatchSize, that.hnswBatchSize)
                && Objects.equals(hnswSyncThreshold, that.hnswSyncThreshold)
                && Objects.equals(hnswResizeFactor, that.hnswResizeFactor)
                && Objects.equals(spannSearchNprobe, that.spannSearchNprobe)
                && Objects.equals(spannEfSearch, that.spannEfSearch)
                && Objects.equals(schema, that.schema)
                && Objects.equals(embeddingFunction, that.embeddingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                space,
                hnswM,
                hnswConstructionEf,
                hnswSearchEf,
                hnswNumThreads,
                hnswBatchSize,
                hnswSyncThreshold,
                hnswResizeFactor,
                spannSearchNprobe,
                spannEfSearch,
                schema,
                embeddingFunction
        );
    }

    @Override
    public String toString() {
        return "CollectionConfiguration{"
                + "space=" + space
                + ", hnswM=" + hnswM
                + ", hnswConstructionEf=" + hnswConstructionEf
                + ", hnswSearchEf=" + hnswSearchEf
                + ", hnswNumThreads=" + hnswNumThreads
                + ", hnswBatchSize=" + hnswBatchSize
                + ", hnswSyncThreshold=" + hnswSyncThreshold
                + ", hnswResizeFactor=" + hnswResizeFactor
                + ", spannSearchNprobe=" + spannSearchNprobe
                + ", spannEfSearch=" + spannEfSearch
                + ", schema=" + schema
                + ", embeddingFunction=" + embeddingFunction
                + '}';
    }

    public static final class Builder {
        private DistanceFunction space;
        private Integer hnswM;
        private Integer hnswConstructionEf;
        private Integer hnswSearchEf;
        private Integer hnswNumThreads;
        private Integer hnswBatchSize;
        private Integer hnswSyncThreshold;
        private Double hnswResizeFactor;
        private Integer spannSearchNprobe;
        private Integer spannEfSearch;
        private Schema schema;
        private EmbeddingFunctionSpec embeddingFunction;

        Builder() {}

        public Builder space(DistanceFunction space) { this.space = space; return this; }
        /** @throws IllegalArgumentException if {@code m <= 0} */
        public Builder hnswM(int m) { this.hnswM = requirePositive("hnswM", m); return this; }
        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder hnswConstructionEf(int ef) { this.hnswConstructionEf = requirePositive("hnswConstructionEf", ef); return this; }
        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder hnswSearchEf(int ef) { this.hnswSearchEf = requirePositive("hnswSearchEf", ef); return this; }
        /** @throws IllegalArgumentException if {@code threads <= 0} */
        public Builder hnswNumThreads(int threads) { this.hnswNumThreads = requirePositive("hnswNumThreads", threads); return this; }
        /** @throws IllegalArgumentException if {@code size < 2} */
        public Builder hnswBatchSize(int size) { this.hnswBatchSize = requireAtLeast("hnswBatchSize", size, 2); return this; }
        /** @throws IllegalArgumentException if {@code threshold < 2} */
        public Builder hnswSyncThreshold(int threshold) { this.hnswSyncThreshold = requireAtLeast("hnswSyncThreshold", threshold, 2); return this; }
        /** @throws IllegalArgumentException if {@code factor <= 0} or not finite */
        public Builder hnswResizeFactor(double factor) {
            this.hnswResizeFactor = Double.valueOf(requirePositiveFinite("hnswResizeFactor", factor));
            return this;
        }
        /** @throws IllegalArgumentException if {@code nprobe <= 0} */
        public Builder spannSearchNprobe(int nprobe) { this.spannSearchNprobe = requirePositive("spannSearchNprobe", nprobe); return this; }
        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder spannEfSearch(int ef) { this.spannEfSearch = requirePositive("spannEfSearch", ef); return this; }
        public Builder schema(Schema schema) { this.schema = schema; return this; }
        public Builder embeddingFunction(EmbeddingFunctionSpec embeddingFunction) {
            this.embeddingFunction = embeddingFunction;
            return this;
        }

        public CollectionConfiguration build() {
            if (hasAnyHnswField() && hasAnySpannField()) {
                throw new IllegalStateException(
                        "CollectionConfiguration cannot define both HNSW and SPANN parameters"
                );
            }
            return new CollectionConfiguration(this);
        }

        private boolean hasAnyHnswField() {
            return hnswM != null
                    || hnswConstructionEf != null
                    || hnswSearchEf != null
                    || hnswNumThreads != null
                    || hnswBatchSize != null
                    || hnswSyncThreshold != null
                    || hnswResizeFactor != null;
        }

        private boolean hasAnySpannField() {
            return spannSearchNprobe != null || spannEfSearch != null;
        }

        private static int requirePositive(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0, got " + value);
            }
            return value;
        }

        private static int requireAtLeast(String name, int value, int min) {
            if (value < min) {
                throw new IllegalArgumentException(name + " must be >= " + min + ", got " + value);
            }
            return value;
        }

        private static double requirePositiveFinite(String name, double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException(name + " must be finite, got " + value);
            }
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0, got " + value);
            }
            return value;
        }
    }
}
