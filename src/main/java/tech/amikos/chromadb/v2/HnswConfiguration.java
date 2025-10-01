package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

public class HnswConfiguration {
    @SerializedName("space")
    private final String space;

    @SerializedName("ef_construction")
    private final Integer efConstruction;

    @SerializedName("ef_search")
    private final Integer efSearch;

    @SerializedName("num_threads")
    private final Integer numThreads;

    @SerializedName("M")
    private final Integer m;

    @SerializedName("resize_factor")
    private final Double resizeFactor;

    @SerializedName("batch_size")
    private final Integer batchSize;

    @SerializedName("sync_threshold")
    private final Integer syncThreshold;

    private HnswConfiguration(Builder builder) {
        this.space = builder.space;
        this.efConstruction = builder.efConstruction;
        this.efSearch = builder.efSearch;
        this.numThreads = builder.numThreads;
        this.m = builder.m;
        this.resizeFactor = builder.resizeFactor;
        this.batchSize = builder.batchSize;
        this.syncThreshold = builder.syncThreshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String space = "l2";
        private Integer efConstruction;
        private Integer efSearch;
        private Integer numThreads;
        private Integer m;
        private Double resizeFactor;
        private Integer batchSize;
        private Integer syncThreshold;

        public Builder space(String space) {
            this.space = space;
            return this;
        }

        public Builder efConstruction(Integer efConstruction) {
            this.efConstruction = efConstruction;
            return this;
        }

        public Builder efSearch(Integer efSearch) {
            this.efSearch = efSearch;
            return this;
        }

        public Builder numThreads(Integer numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        public Builder m(Integer m) {
            this.m = m;
            return this;
        }

        public Builder resizeFactor(Double resizeFactor) {
            this.resizeFactor = resizeFactor;
            return this;
        }

        public Builder batchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder syncThreshold(Integer syncThreshold) {
            this.syncThreshold = syncThreshold;
            return this;
        }

        public HnswConfiguration build() {
            return new HnswConfiguration(this);
        }
    }
}