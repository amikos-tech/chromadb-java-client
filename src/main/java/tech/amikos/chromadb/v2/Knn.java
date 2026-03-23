package tech.amikos.chromadb.v2;

import java.util.Arrays;

/**
 * KNN (K-Nearest Neighbor) ranking expression for the Search API.
 *
 * <p>Use the static factory methods to create an initial instance, then chain fluent setters to
 * configure optional parameters. All chainable methods return a new immutable instance.</p>
 *
 * <pre>{@code
 * // Text-based KNN
 * Knn knn = Knn.queryText("search query").limit(10);
 *
 * // Embedding-based KNN
 * Knn knn = Knn.queryEmbedding(new float[]{0.1f, 0.2f}).limit(10);
 *
 * // Sparse vector KNN
 * Knn knn = Knn.querySparseVector(SparseVector.of(indices, values)).key("sparse_field");
 * }</pre>
 */
public final class Knn {

    private final Object query;
    private final String key;
    private final Integer limit;
    private final Double defaultScore;
    private final boolean returnRank;

    private Knn(Object query, String key, Integer limit, Double defaultScore, boolean returnRank) {
        this.query = query;
        this.key = key;
        this.limit = limit;
        this.defaultScore = defaultScore;
        this.returnRank = returnRank;
    }

    /**
     * Creates a KNN query by text. The text is sent to the server, which uses the collection's
     * server-side embedding function to convert it to an embedding.
     *
     * <p>Unlike {@link Collection.QueryBuilder#queryTexts(String...)}, no client-side embedding
     * function is invoked.</p>
     *
     * @param text the query text; must not be null
     * @return a new {@code Knn} instance
     * @throws IllegalArgumentException if {@code text} is null
     */
    public static Knn queryText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return new Knn(text, "#embedding", null, null, false);
    }

    /**
     * Creates a KNN query by raw embedding vector. A defensive copy is made.
     *
     * @param embedding the query embedding; must not be null
     * @return a new {@code Knn} instance
     * @throws IllegalArgumentException if {@code embedding} is null
     */
    public static Knn queryEmbedding(float[] embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("embedding must not be null");
        }
        return new Knn(Arrays.copyOf(embedding, embedding.length), "#embedding", null, null, false);
    }

    /**
     * Creates a KNN query by sparse vector. The {@code key} field defaults to {@code null} and
     * should be set via {@link #key(String)} to identify the target sparse field. If omitted,
     * the key will not be included in the wire format.
     *
     * @param sparseVector the sparse query vector; must not be null
     * @return a new {@code Knn} instance
     * @throws IllegalArgumentException if {@code sparseVector} is null
     */
    public static Knn querySparseVector(SparseVector sparseVector) {
        if (sparseVector == null) {
            throw new IllegalArgumentException("sparseVector must not be null");
        }
        return new Knn(sparseVector, null, null, null, false);
    }

    /**
     * Returns a copy of this instance with the given query key (e.g., {@code "#embedding"} or a
     * named sparse field).
     *
     * @param key the target field key; must not be null
     * @return new {@code Knn} with the key set
     */
    public Knn key(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return new Knn(this.query, key, this.limit, this.defaultScore, this.returnRank);
    }

    /**
     * Returns a copy of this instance with the given per-rank result limit.
     *
     * @param limit maximum number of results to return for this rank
     * @return new {@code Knn} with limit set
     */
    public Knn limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        return new Knn(this.query, this.key, limit, this.defaultScore, this.returnRank);
    }

    /**
     * Returns a copy of this instance with the given default score for missing results.
     *
     * @param score default score value
     * @return new {@code Knn} with defaultScore set
     */
    public Knn defaultScore(double score) {
        return new Knn(this.query, this.key, this.limit, score, this.returnRank);
    }

    /**
     * Returns a copy of this instance with the return_rank flag set.
     *
     * <p>This is required for sub-rankings used inside {@link Rrf} and is automatically set by
     * {@link Rrf.Builder#rank(Knn, double)}. Only set this manually when constructing standalone
     * KNN queries that need rank in the result.</p>
     *
     * @param returnRank whether to include rank position in results
     * @return new {@code Knn} with returnRank set
     */
    public Knn returnRank(boolean returnRank) {
        return new Knn(this.query, this.key, this.limit, this.defaultScore, returnRank);
    }

    /**
     * Returns a copy of this instance with {@code returnRank=true}.
     *
     * <p>Package-private; used by {@link Rrf.Builder} to auto-configure sub-rankings.</p>
     */
    Knn withReturnRank() {
        return new Knn(this.query, this.key, this.limit, this.defaultScore, true);
    }

    /**
     * Returns the query object (String, float[], or {@link SparseVector}).
     * When the query is a {@code float[]}, a defensive copy is returned.
     */
    public Object getQuery() {
        if (query instanceof float[]) {
            return Arrays.copyOf((float[]) query, ((float[]) query).length);
        }
        return query;
    }

    /**
     * Returns the target field key, or {@code null} if not set (sparse vector case).
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the per-rank result limit, or {@code null} if not set.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Returns the default score for missing results, or {@code null} if not set.
     */
    public Double getDefaultScore() {
        return defaultScore;
    }

    /**
     * Returns whether the rank position should be included in results.
     */
    public boolean isReturnRank() {
        return returnRank;
    }
}
