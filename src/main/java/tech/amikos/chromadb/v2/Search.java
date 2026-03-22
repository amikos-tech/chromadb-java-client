package tech.amikos.chromadb.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Per-search configuration composing a ranking expression (KNN or RRF) with optional filter,
 * field projection, grouping, limit, and offset.
 *
 * <p>Exactly one of {@link Builder#knn(Knn)} or {@link Builder#rrf(Rrf)} must be set.</p>
 *
 * <pre>{@code
 * Search search = Search.builder()
 *     .knn(Knn.queryText("chromadb java client"))
 *     .where(Where.eq("category", "tech"))
 *     .selectAll()
 *     .limit(10)
 *     .build();
 * }</pre>
 */
public final class Search {

    private final Knn knn;
    private final Rrf rrf;
    private final Where filter;
    private final List<Select> select;
    private final GroupBy groupBy;
    private final Integer limit;
    private final Integer offset;

    private Search(Builder builder) {
        this.knn = builder.knn;
        this.rrf = builder.rrf;
        this.filter = builder.filter;
        this.select = builder.select == null
                ? null
                : Collections.unmodifiableList(Arrays.asList(builder.select));
        this.groupBy = builder.groupBy;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    /**
     * Returns a new {@link Builder} for constructing a {@code Search} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the KNN ranking expression, or {@code null} if RRF is used.
     */
    public Knn getKnn() {
        return knn;
    }

    /**
     * Returns the RRF ranking expression, or {@code null} if KNN is used.
     */
    public Rrf getRrf() {
        return rrf;
    }

    /**
     * Returns the per-search metadata/ID filter, or {@code null} if not set.
     */
    public Where getFilter() {
        return filter;
    }

    /**
     * Returns the unmodifiable list of field projections, or {@code null} if not set.
     */
    public List<Select> getSelect() {
        return select;
    }

    /**
     * Returns the groupBy configuration, or {@code null} if not set.
     */
    public GroupBy getGroupBy() {
        return groupBy;
    }

    /**
     * Returns the per-search result limit, or {@code null} if not set.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Returns the per-search result offset, or {@code null} if not set.
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Builder for {@link Search}.
     */
    public static final class Builder {

        private Knn knn;
        private Rrf rrf;
        private Where filter;
        private Select[] select;
        private GroupBy groupBy;
        private Integer limit;
        private Integer offset;

        private Builder() {}

        /**
         * Sets the KNN ranking expression. Mutually exclusive with {@link #rrf(Rrf)}.
         *
         * @param knn KNN ranking expression; must not be null
         * @return this builder
         */
        public Builder knn(Knn knn) {
            this.knn = knn;
            return this;
        }

        /**
         * Sets the RRF ranking expression. Mutually exclusive with {@link #knn(Knn)}.
         *
         * @param rrf RRF ranking expression; must not be null
         * @return this builder
         */
        public Builder rrf(Rrf rrf) {
            this.rrf = rrf;
            return this;
        }

        /**
         * Sets a per-search metadata/ID filter.
         *
         * @param filter the where filter; must not be null
         * @return this builder
         */
        public Builder where(Where filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets per-search field projection.
         *
         * @param fields one or more field selectors to project
         * @return this builder
         */
        public Builder select(Select... fields) {
            this.select = fields;
            return this;
        }

        /**
         * Convenience method that projects all standard fields: ID, DOCUMENT, EMBEDDING,
         * METADATA, SCORE.
         *
         * @return this builder
         */
        public Builder selectAll() {
            return select(Select.all());
        }

        /**
         * Sets per-search result grouping.
         *
         * @param groupBy the group-by configuration; must not be null
         * @return this builder
         */
        public Builder groupBy(GroupBy groupBy) {
            this.groupBy = groupBy;
            return this;
        }

        /**
         * Sets the per-search result limit.
         *
         * @param limit maximum number of results to return
         * @return this builder
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the per-search result offset.
         *
         * @param offset number of results to skip
         * @return this builder
         */
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Builds the {@link Search} instance.
         *
         * @return an immutable {@code Search}
         * @throws IllegalArgumentException if neither or both of knn and rrf are set
         */
        public Search build() {
            if (knn == null && rrf == null) {
                throw new IllegalArgumentException(
                        "exactly one of knn or rrf must be set, but neither was provided");
            }
            if (knn != null && rrf != null) {
                throw new IllegalArgumentException(
                        "exactly one of knn or rrf must be set, but both were provided");
            }
            return new Search(this);
        }
    }
}
