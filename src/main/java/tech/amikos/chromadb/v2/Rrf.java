package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reciprocal Rank Fusion (RRF) ranking expression that combines multiple KNN rankings.
 *
 * <p>RRF fuses multiple ranked lists into a single ranking by assigning reciprocal rank scores.
 * At least one sub-ranking is required. All {@link Knn} sub-rankings have {@code returnRank}
 * automatically enabled when added via {@link Builder#rank(Knn, double)}.</p>
 *
 * <pre>{@code
 * Rrf rrf = Rrf.builder()
 *     .rank(Knn.queryText("query"), 1.0)
 *     .rank(Knn.querySparseVector(sv).key("sparse"), 0.5)
 *     .k(60)
 *     .build();
 * }</pre>
 */
public final class Rrf {

    private final List<RankWithWeight> ranks;
    private final int k;
    private final boolean normalize;

    private Rrf(List<RankWithWeight> ranks, int k, boolean normalize) {
        this.ranks = Collections.unmodifiableList(new ArrayList<RankWithWeight>(ranks));
        this.k = k;
        this.normalize = normalize;
    }

    /**
     * Returns a new {@link Builder} for constructing an {@code Rrf} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the unmodifiable list of ranked inputs with their weights.
     */
    public List<RankWithWeight> getRanks() {
        return ranks;
    }

    /**
     * Returns the RRF k constant (default 60).
     */
    public int getK() {
        return k;
    }

    /**
     * Returns whether scores should be normalized.
     */
    public boolean isNormalize() {
        return normalize;
    }

    /**
     * A KNN sub-ranking paired with a fusion weight.
     */
    public static final class RankWithWeight {

        private final Knn knn;
        private final double weight;

        private RankWithWeight(Knn knn, double weight) {
            this.knn = knn;
            this.weight = weight;
        }

        /**
         * Returns the KNN sub-ranking.
         */
        public Knn getKnn() {
            return knn;
        }

        /**
         * Returns the fusion weight for this ranking.
         */
        public double getWeight() {
            return weight;
        }
    }

    /**
     * Builder for {@link Rrf}.
     */
    public static final class Builder {

        private final List<RankWithWeight> ranks = new ArrayList<RankWithWeight>();
        private int k = 60;
        private boolean normalize = false;

        private Builder() {}

        /**
         * Adds a KNN sub-ranking with the given weight. The {@code returnRank} flag is
         * automatically set to {@code true} on the provided {@link Knn} instance.
         *
         * @param knn    the KNN sub-ranking; must not be null
         * @param weight fusion weight for this sub-ranking
         * @return this builder
         * @throws IllegalArgumentException if {@code knn} is null
         */
        public Builder rank(Knn knn, double weight) {
            if (knn == null) {
                throw new IllegalArgumentException("knn must not be null");
            }
            ranks.add(new RankWithWeight(knn.withReturnRank(), weight));
            return this;
        }

        /**
         * Sets the RRF k constant. Default is 60.
         *
         * @param k the RRF k constant
         * @return this builder
         */
        public Builder k(int k) {
            this.k = k;
            return this;
        }

        /**
         * Sets whether scores should be normalized. Default is {@code false}.
         *
         * @param normalize whether to normalize scores
         * @return this builder
         */
        public Builder normalize(boolean normalize) {
            this.normalize = normalize;
            return this;
        }

        /**
         * Builds the {@link Rrf} instance.
         *
         * @return an immutable {@code Rrf}
         * @throws IllegalArgumentException if no ranks have been added
         */
        public Rrf build() {
            if (ranks.isEmpty()) {
                throw new IllegalArgumentException("at least one rank must be added");
            }
            return new Rrf(ranks, k, normalize);
        }
    }
}
