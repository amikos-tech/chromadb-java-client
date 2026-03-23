package tech.amikos.chromadb.v2;

/**
 * A result row from a search operation, extending {@link ResultRow} with a score field.
 *
 * <p>{@link #getScore()} returns {@code null} when {@link Select#SCORE} was not included in the
 * search request's field projection.</p>
 */
public interface SearchResultRow extends ResultRow {

    /**
     * Returns the relevance score from the ranking expression, or {@code null} if
     * {@link Select#SCORE} was not included in the projection.
     *
     * <p>Higher scores indicate greater relevance. Returns {@link Double} to preserve
     * the full wire-format precision, consistent with {@link SearchResult#getScores()}.</p>
     */
    Double getScore();
}
