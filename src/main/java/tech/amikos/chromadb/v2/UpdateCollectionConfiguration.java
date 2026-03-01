package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable runtime collection configuration update payload (HNSW or SPANN). */
public final class UpdateCollectionConfiguration {

    private final Integer hnswSearchEf;
    private final Integer hnswNumThreads;
    private final Integer hnswBatchSize;
    private final Integer hnswSyncThreshold;
    private final Double hnswResizeFactor;
    private final Integer spannSearchNprobe;
    private final Integer spannEfSearch;

    private UpdateCollectionConfiguration(Builder builder) {
        this.hnswSearchEf = builder.hnswSearchEf;
        this.hnswNumThreads = builder.hnswNumThreads;
        this.hnswBatchSize = builder.hnswBatchSize;
        this.hnswSyncThreshold = builder.hnswSyncThreshold;
        this.hnswResizeFactor = builder.hnswResizeFactor;
        this.spannSearchNprobe = builder.spannSearchNprobe;
        this.spannEfSearch = builder.spannEfSearch;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getHnswSearchEf() { return hnswSearchEf; }
    public Integer getHnswNumThreads() { return hnswNumThreads; }
    public Integer getHnswBatchSize() { return hnswBatchSize; }
    public Integer getHnswSyncThreshold() { return hnswSyncThreshold; }
    public Double getHnswResizeFactor() { return hnswResizeFactor; }
    public Integer getSpannSearchNprobe() { return spannSearchNprobe; }
    public Integer getSpannEfSearch() { return spannEfSearch; }

    public boolean hasHnswUpdates() {
        return hnswSearchEf != null
                || hnswNumThreads != null
                || hnswBatchSize != null
                || hnswSyncThreshold != null
                || hnswResizeFactor != null;
    }

    public boolean hasSpannUpdates() {
        return spannSearchNprobe != null || spannEfSearch != null;
    }

    public void validate() {
        boolean hasHnsw = hasHnswUpdates();
        boolean hasSpann = hasSpannUpdates();
        if (!hasHnsw && !hasSpann) {
            throw new IllegalArgumentException(
                    "configuration must specify at least one parameter to modify (hnsw or spann)"
            );
        }
        if (hasHnsw && hasSpann) {
            throw new IllegalArgumentException(
                    "cannot update both hnsw and spann configuration in the same request"
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateCollectionConfiguration)) return false;
        UpdateCollectionConfiguration that = (UpdateCollectionConfiguration) o;
        return Objects.equals(hnswSearchEf, that.hnswSearchEf)
                && Objects.equals(hnswNumThreads, that.hnswNumThreads)
                && Objects.equals(hnswBatchSize, that.hnswBatchSize)
                && Objects.equals(hnswSyncThreshold, that.hnswSyncThreshold)
                && Objects.equals(hnswResizeFactor, that.hnswResizeFactor)
                && Objects.equals(spannSearchNprobe, that.spannSearchNprobe)
                && Objects.equals(spannEfSearch, that.spannEfSearch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hnswSearchEf,
                hnswNumThreads,
                hnswBatchSize,
                hnswSyncThreshold,
                hnswResizeFactor,
                spannSearchNprobe,
                spannEfSearch
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("UpdateCollectionConfiguration{");
        boolean hasContent = false;
        if (hasHnswUpdates()) {
            sb.append("hnsw={")
                    .append("efSearch=").append(hnswSearchEf)
                    .append(", numThreads=").append(hnswNumThreads)
                    .append(", batchSize=").append(hnswBatchSize)
                    .append(", syncThreshold=").append(hnswSyncThreshold)
                    .append(", resizeFactor=").append(hnswResizeFactor)
                    .append('}');
            hasContent = true;
        }
        if (hasSpannUpdates()) {
            if (hasContent) {
                sb.append(", ");
            }
            sb.append("spann={")
                    .append("searchNprobe=").append(spannSearchNprobe)
                    .append(", efSearch=").append(spannEfSearch)
                    .append('}');
            hasContent = true;
        }
        if (!hasContent) {
            sb.append("empty");
        }
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        private Integer hnswSearchEf;
        private Integer hnswNumThreads;
        private Integer hnswBatchSize;
        private Integer hnswSyncThreshold;
        private Double hnswResizeFactor;
        private Integer spannSearchNprobe;
        private Integer spannEfSearch;

        Builder() {}

        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder hnswSearchEf(int ef) {
            this.hnswSearchEf = requirePositive("hnswSearchEf", ef);
            return this;
        }

        /** @throws IllegalArgumentException if {@code threads <= 0} */
        public Builder hnswNumThreads(int threads) {
            this.hnswNumThreads = requirePositive("hnswNumThreads", threads);
            return this;
        }

        /** @throws IllegalArgumentException if {@code size <= 0} */
        public Builder hnswBatchSize(int size) {
            this.hnswBatchSize = requirePositive("hnswBatchSize", size);
            return this;
        }

        /** @throws IllegalArgumentException if {@code threshold <= 0} */
        public Builder hnswSyncThreshold(int threshold) {
            this.hnswSyncThreshold = requirePositive("hnswSyncThreshold", threshold);
            return this;
        }

        /**
         * @throws IllegalArgumentException if {@code factor <= 0} or is not finite
         */
        public Builder hnswResizeFactor(double factor) {
            this.hnswResizeFactor = Double.valueOf(requirePositiveFinite("hnswResizeFactor", factor));
            return this;
        }

        /** @throws IllegalArgumentException if {@code nprobe <= 0} */
        public Builder spannSearchNprobe(int nprobe) {
            this.spannSearchNprobe = requirePositive("spannSearchNprobe", nprobe);
            return this;
        }

        /** @throws IllegalArgumentException if {@code ef <= 0} */
        public Builder spannEfSearch(int ef) {
            this.spannEfSearch = requirePositive("spannEfSearch", ef);
            return this;
        }

        public UpdateCollectionConfiguration build() {
            UpdateCollectionConfiguration config = new UpdateCollectionConfiguration(this);
            config.validate();
            return config;
        }

        private static int requirePositive(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0, got " + value);
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
