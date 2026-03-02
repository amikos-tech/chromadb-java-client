package tech.amikos.chromadb.v2;

import java.util.Objects;

/** SPANN index configuration for Chroma Cloud schema vector index settings. */
public final class SpannIndexConfig {

    private final Integer searchNprobe;
    private final Double searchRngFactor;
    private final Double searchRngEpsilon;
    private final Integer nreplicaCount;
    private final Double writeRngFactor;
    private final Double writeRngEpsilon;
    private final Integer splitThreshold;
    private final Integer numSamplesKmeans;
    private final Double initialLambda;
    private final Integer reassignNeighborCount;
    private final Integer mergeThreshold;
    private final Integer numCentersToMergeTo;
    private final Integer writeNprobe;
    private final Integer efConstruction;
    private final Integer efSearch;
    private final Integer maxNeighbors;
    private final SpannQuantization quantize;

    private SpannIndexConfig(Builder builder) {
        this.searchNprobe = builder.searchNprobe;
        this.searchRngFactor = builder.searchRngFactor;
        this.searchRngEpsilon = builder.searchRngEpsilon;
        this.nreplicaCount = builder.nreplicaCount;
        this.writeRngFactor = builder.writeRngFactor;
        this.writeRngEpsilon = builder.writeRngEpsilon;
        this.splitThreshold = builder.splitThreshold;
        this.numSamplesKmeans = builder.numSamplesKmeans;
        this.initialLambda = builder.initialLambda;
        this.reassignNeighborCount = builder.reassignNeighborCount;
        this.mergeThreshold = builder.mergeThreshold;
        this.numCentersToMergeTo = builder.numCentersToMergeTo;
        this.writeNprobe = builder.writeNprobe;
        this.efConstruction = builder.efConstruction;
        this.efSearch = builder.efSearch;
        this.maxNeighbors = builder.maxNeighbors;
        this.quantize = builder.quantize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getSearchNprobe() { return searchNprobe; }
    public Double getSearchRngFactor() { return searchRngFactor; }
    public Double getSearchRngEpsilon() { return searchRngEpsilon; }
    public Integer getNreplicaCount() { return nreplicaCount; }
    public Double getWriteRngFactor() { return writeRngFactor; }
    public Double getWriteRngEpsilon() { return writeRngEpsilon; }
    public Integer getSplitThreshold() { return splitThreshold; }
    public Integer getNumSamplesKmeans() { return numSamplesKmeans; }
    public Double getInitialLambda() { return initialLambda; }
    public Integer getReassignNeighborCount() { return reassignNeighborCount; }
    public Integer getMergeThreshold() { return mergeThreshold; }
    public Integer getNumCentersToMergeTo() { return numCentersToMergeTo; }
    public Integer getWriteNprobe() { return writeNprobe; }
    public Integer getEfConstruction() { return efConstruction; }
    public Integer getEfSearch() { return efSearch; }
    public Integer getMaxNeighbors() { return maxNeighbors; }
    public SpannQuantization getQuantize() { return quantize; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpannIndexConfig)) return false;
        SpannIndexConfig that = (SpannIndexConfig) o;
        return Objects.equals(searchNprobe, that.searchNprobe)
                && Objects.equals(searchRngFactor, that.searchRngFactor)
                && Objects.equals(searchRngEpsilon, that.searchRngEpsilon)
                && Objects.equals(nreplicaCount, that.nreplicaCount)
                && Objects.equals(writeRngFactor, that.writeRngFactor)
                && Objects.equals(writeRngEpsilon, that.writeRngEpsilon)
                && Objects.equals(splitThreshold, that.splitThreshold)
                && Objects.equals(numSamplesKmeans, that.numSamplesKmeans)
                && Objects.equals(initialLambda, that.initialLambda)
                && Objects.equals(reassignNeighborCount, that.reassignNeighborCount)
                && Objects.equals(mergeThreshold, that.mergeThreshold)
                && Objects.equals(numCentersToMergeTo, that.numCentersToMergeTo)
                && Objects.equals(writeNprobe, that.writeNprobe)
                && Objects.equals(efConstruction, that.efConstruction)
                && Objects.equals(efSearch, that.efSearch)
                && Objects.equals(maxNeighbors, that.maxNeighbors)
                && quantize == that.quantize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                searchNprobe,
                searchRngFactor,
                searchRngEpsilon,
                nreplicaCount,
                writeRngFactor,
                writeRngEpsilon,
                splitThreshold,
                numSamplesKmeans,
                initialLambda,
                reassignNeighborCount,
                mergeThreshold,
                numCentersToMergeTo,
                writeNprobe,
                efConstruction,
                efSearch,
                maxNeighbors,
                quantize
        );
    }

    @Override
    public String toString() {
        return "SpannIndexConfig{"
                + "searchNprobe=" + searchNprobe
                + ", searchRngFactor=" + searchRngFactor
                + ", searchRngEpsilon=" + searchRngEpsilon
                + ", nreplicaCount=" + nreplicaCount
                + ", writeRngFactor=" + writeRngFactor
                + ", writeRngEpsilon=" + writeRngEpsilon
                + ", splitThreshold=" + splitThreshold
                + ", numSamplesKmeans=" + numSamplesKmeans
                + ", initialLambda=" + initialLambda
                + ", reassignNeighborCount=" + reassignNeighborCount
                + ", mergeThreshold=" + mergeThreshold
                + ", numCentersToMergeTo=" + numCentersToMergeTo
                + ", writeNprobe=" + writeNprobe
                + ", efConstruction=" + efConstruction
                + ", efSearch=" + efSearch
                + ", maxNeighbors=" + maxNeighbors
                + ", quantize=" + quantize
                + '}';
    }

    public static final class Builder {
        private Integer searchNprobe;
        private Double searchRngFactor;
        private Double searchRngEpsilon;
        private Integer nreplicaCount;
        private Double writeRngFactor;
        private Double writeRngEpsilon;
        private Integer splitThreshold;
        private Integer numSamplesKmeans;
        private Double initialLambda;
        private Integer reassignNeighborCount;
        private Integer mergeThreshold;
        private Integer numCentersToMergeTo;
        private Integer writeNprobe;
        private Integer efConstruction;
        private Integer efSearch;
        private Integer maxNeighbors;
        private SpannQuantization quantize;

        Builder() {}

        public Builder searchNprobe(int value) { this.searchNprobe = requireRange("searchNprobe", value, 1, 128); return this; }
        public Builder searchRngFactor(double value) { this.searchRngFactor = Double.valueOf(requireFinite("searchRngFactor", value)); return this; }
        public Builder searchRngEpsilon(double value) { this.searchRngEpsilon = Double.valueOf(requireRange("searchRngEpsilon", value, 5.0, 10.0)); return this; }
        public Builder nreplicaCount(int value) { this.nreplicaCount = requireRange("nreplicaCount", value, 1, 8); return this; }
        public Builder writeRngFactor(double value) { this.writeRngFactor = Double.valueOf(requireFinite("writeRngFactor", value)); return this; }
        public Builder writeRngEpsilon(double value) { this.writeRngEpsilon = Double.valueOf(requireRange("writeRngEpsilon", value, 5.0, 10.0)); return this; }
        public Builder splitThreshold(int value) { this.splitThreshold = requireRange("splitThreshold", value, 50, 200); return this; }
        public Builder numSamplesKmeans(int value) { this.numSamplesKmeans = requireRange("numSamplesKmeans", value, 1, 1000); return this; }
        public Builder initialLambda(double value) { this.initialLambda = Double.valueOf(requireFinite("initialLambda", value)); return this; }
        public Builder reassignNeighborCount(int value) { this.reassignNeighborCount = requireRange("reassignNeighborCount", value, 1, 64); return this; }
        public Builder mergeThreshold(int value) { this.mergeThreshold = requireRange("mergeThreshold", value, 25, 100); return this; }
        public Builder numCentersToMergeTo(int value) { this.numCentersToMergeTo = requireRange("numCentersToMergeTo", value, 1, 8); return this; }
        public Builder writeNprobe(int value) { this.writeNprobe = requireRange("writeNprobe", value, 1, 64); return this; }
        public Builder efConstruction(int value) { this.efConstruction = requireRange("efConstruction", value, 1, 200); return this; }
        public Builder efSearch(int value) { this.efSearch = requireRange("efSearch", value, 1, 200); return this; }
        public Builder maxNeighbors(int value) { this.maxNeighbors = requireRange("maxNeighbors", value, 1, 64); return this; }
        public Builder quantize(SpannQuantization value) { this.quantize = value; return this; }

        public SpannIndexConfig build() {
            return new SpannIndexConfig(this);
        }

        private static int requireRange(String name, int value, int min, int max) {
            if (value < min || value > max) {
                throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "], got " + value);
            }
            return value;
        }

        private static double requireRange(String name, double value, double min, double max) {
            if (Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max) {
                throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "] and finite, got " + value);
            }
            return value;
        }

        private static double requireFinite(String name, double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException(name + " must be finite, got " + value);
            }
            return value;
        }
    }
}
