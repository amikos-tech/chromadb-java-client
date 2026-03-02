package tech.amikos.chromadb.v2;

import java.util.Objects;

/** HNSW index configuration for schema vector index settings. */
public final class HnswIndexConfig {

    private final Integer efConstruction;
    private final Integer maxNeighbors;
    private final Integer efSearch;
    private final Integer numThreads;
    private final Integer batchSize;
    private final Integer syncThreshold;
    private final Double resizeFactor;

    private HnswIndexConfig(Builder builder) {
        this.efConstruction = builder.efConstruction;
        this.maxNeighbors = builder.maxNeighbors;
        this.efSearch = builder.efSearch;
        this.numThreads = builder.numThreads;
        this.batchSize = builder.batchSize;
        this.syncThreshold = builder.syncThreshold;
        this.resizeFactor = builder.resizeFactor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getEfConstruction() {
        return efConstruction;
    }

    public Integer getMaxNeighbors() {
        return maxNeighbors;
    }

    public Integer getEfSearch() {
        return efSearch;
    }

    public Integer getNumThreads() {
        return numThreads;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public Integer getSyncThreshold() {
        return syncThreshold;
    }

    public Double getResizeFactor() {
        return resizeFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HnswIndexConfig)) return false;
        HnswIndexConfig that = (HnswIndexConfig) o;
        return Objects.equals(efConstruction, that.efConstruction)
                && Objects.equals(maxNeighbors, that.maxNeighbors)
                && Objects.equals(efSearch, that.efSearch)
                && Objects.equals(numThreads, that.numThreads)
                && Objects.equals(batchSize, that.batchSize)
                && Objects.equals(syncThreshold, that.syncThreshold)
                && Objects.equals(resizeFactor, that.resizeFactor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(efConstruction, maxNeighbors, efSearch, numThreads, batchSize, syncThreshold, resizeFactor);
    }

    @Override
    public String toString() {
        return "HnswIndexConfig{"
                + "efConstruction=" + efConstruction
                + ", maxNeighbors=" + maxNeighbors
                + ", efSearch=" + efSearch
                + ", numThreads=" + numThreads
                + ", batchSize=" + batchSize
                + ", syncThreshold=" + syncThreshold
                + ", resizeFactor=" + resizeFactor
                + '}';
    }

    public static final class Builder {
        private Integer efConstruction;
        private Integer maxNeighbors;
        private Integer efSearch;
        private Integer numThreads;
        private Integer batchSize;
        private Integer syncThreshold;
        private Double resizeFactor;

        Builder() {}

        public Builder efConstruction(int value) {
            this.efConstruction = requirePositive("efConstruction", value);
            return this;
        }

        public Builder maxNeighbors(int value) {
            this.maxNeighbors = requirePositive("maxNeighbors", value);
            return this;
        }

        public Builder efSearch(int value) {
            this.efSearch = requirePositive("efSearch", value);
            return this;
        }

        public Builder numThreads(int value) {
            this.numThreads = requirePositive("numThreads", value);
            return this;
        }

        public Builder batchSize(int value) {
            this.batchSize = requireAtLeast("batchSize", value, 2);
            return this;
        }

        public Builder syncThreshold(int value) {
            this.syncThreshold = requireAtLeast("syncThreshold", value, 2);
            return this;
        }

        public Builder resizeFactor(double value) {
            this.resizeFactor = Double.valueOf(requirePositiveFinite("resizeFactor", value));
            return this;
        }

        public HnswIndexConfig build() {
            return new HnswIndexConfig(this);
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
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0 and finite, got " + value);
            }
            return value;
        }
    }
}
