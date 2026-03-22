package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Result from a search operation.
 *
 * <p>Supports both column-oriented access (lists of lists, indexed by search input) and
 * row-oriented access (result groups and rows).</p>
 *
 * <p>Each outer list is indexed per search input. Fields omitted from the {@link Select}
 * projection may be {@code null}.</p>
 */
public interface SearchResult {

    // --- Column-oriented accessors ---

    /**
     * Always present. Each inner list corresponds to one search input.
     */
    List<List<String>> getIds();

    /**
     * Present when {@link Select#DOCUMENT} is projected; otherwise may be {@code null}.
     */
    List<List<String>> getDocuments();

    /**
     * Present when {@link Select#METADATA} is projected; otherwise may be {@code null}.
     */
    List<List<Map<String, Object>>> getMetadatas();

    /**
     * Present when {@link Select#EMBEDDING} is projected; otherwise may be {@code null}.
     */
    List<List<float[]>> getEmbeddings();

    /**
     * Present when {@link Select#SCORE} is projected; otherwise may be {@code null}.
     *
     * <p>Scores are {@link Double} (not {@code Float}) to match the wire format precision.
     * Higher values indicate greater relevance.</p>
     */
    List<List<Double>> getScores();

    // --- Row-oriented accessors ---

    /**
     * Returns the results for the specified search input as a flat row-oriented group.
     *
     * @param searchIndex zero-based index of the search input
     * @return group of rows for that search input
     * @throws IndexOutOfBoundsException if searchIndex is out of range
     */
    ResultGroup<SearchResultRow> rows(int searchIndex);

    /**
     * Returns the grouped results for the specified search input.
     *
     * <p>Returns a non-empty list when {@link GroupBy} was configured and the server returned
     * grouped results. Returns an empty list when grouping was not configured.</p>
     *
     * @param searchIndex zero-based index of the search input
     * @return list of groups for that search input; empty if not grouped
     * @throws IndexOutOfBoundsException if searchIndex is out of range
     */
    List<SearchResultGroup> groups(int searchIndex);

    /**
     * Returns {@code true} if the results are grouped (i.e., {@link GroupBy} was configured).
     */
    boolean isGrouped();

    /**
     * Returns the number of search inputs (outer list size of ids).
     */
    int groupCount();

    /**
     * Returns a stream over all search groups, enabling flatMap patterns.
     */
    Stream<ResultGroup<SearchResultRow>> stream();
}
