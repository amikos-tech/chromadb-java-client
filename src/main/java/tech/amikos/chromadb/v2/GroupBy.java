package tech.amikos.chromadb.v2;

import java.util.Objects;

/**
 * Configuration for grouping search results by a metadata key.
 *
 * <p>When a {@code GroupBy} is set on a {@link Search}, results are partitioned by the distinct
 * values of the specified metadata key. Optional {@code minK} and {@code maxK} bounds control
 * how many records per group are returned.</p>
 *
 * <p>Instances are immutable and thread-safe. Use {@link #builder()} to construct.</p>
 */
public final class GroupBy {

    private final String key;
    private final Integer minK;
    private final Integer maxK;

    private GroupBy(String key, Integer minK, Integer maxK) {
        this.key = key;
        this.minK = minK;
        this.maxK = maxK;
    }

    /**
     * Returns a new {@link Builder} for constructing a {@code GroupBy} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the metadata key to group by.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the minimum number of results per group, or {@code null} if not set.
     */
    public Integer getMinK() {
        return minK;
    }

    /**
     * Returns the maximum number of results per group, or {@code null} if not set.
     */
    public Integer getMaxK() {
        return maxK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupBy)) return false;
        GroupBy that = (GroupBy) o;
        return Objects.equals(key, that.key)
                && Objects.equals(minK, that.minK)
                && Objects.equals(maxK, that.maxK);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, minK, maxK);
    }

    @Override
    public String toString() {
        return "GroupBy{key='" + key + "', minK=" + minK + ", maxK=" + maxK + '}';
    }

    /**
     * Builder for {@link GroupBy}.
     */
    public static final class Builder {

        private String key;
        private Integer minK;
        private Integer maxK;

        private Builder() {}

        /**
         * Sets the metadata key to group by. Required.
         *
         * @param key non-null, non-blank metadata key
         * @return this builder
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the minimum number of results per group.
         *
         * @param minK minimum results per group
         * @return this builder
         */
        public Builder minK(int minK) {
            this.minK = minK;
            return this;
        }

        /**
         * Sets the maximum number of results per group.
         *
         * @param maxK maximum results per group
         * @return this builder
         */
        public Builder maxK(int maxK) {
            this.maxK = maxK;
            return this;
        }

        /**
         * Builds the {@link GroupBy} instance.
         *
         * @return an immutable {@code GroupBy}
         * @throws IllegalArgumentException if {@code key} is null or blank
         */
        public GroupBy build() {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("key must not be null or blank");
            }
            return new GroupBy(key, minK, maxK);
        }
    }
}
