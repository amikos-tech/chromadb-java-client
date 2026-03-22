package tech.amikos.chromadb.v2;

/**
 * A group of search result rows sharing the same groupBy metadata key value.
 *
 * <p>Returned by {@link SearchResult#groups(int)} when a {@link GroupBy} was configured on the
 * search. Each group corresponds to a distinct value of the groupBy key.</p>
 */
public interface SearchResultGroup {

    /**
     * Returns the metadata value that all rows in this group share.
     */
    Object getKey();

    /**
     * Returns the rows in this group as an ordered, iterable result group.
     */
    ResultGroup<SearchResultRow> rows();
}
