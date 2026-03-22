package tech.amikos.chromadb.v2;

import java.util.Objects;

/**
 * Package-private immutable implementation of {@link SearchResultGroup}.
 */
final class SearchResultGroupImpl implements SearchResultGroup {

    private final Object key;
    private final ResultGroup<SearchResultRow> rows;

    SearchResultGroupImpl(Object key, ResultGroup<SearchResultRow> rows) {
        this.key = key;
        this.rows = rows;
    }

    @Override
    public Object getKey() {
        return key;
    }

    @Override
    public ResultGroup<SearchResultRow> rows() {
        return rows;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SearchResultGroupImpl)) return false;
        SearchResultGroupImpl other = (SearchResultGroupImpl) obj;
        return Objects.equals(key, other.key) && Objects.equals(rows, other.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, rows);
    }

    @Override
    public String toString() {
        return "SearchResultGroup{key=" + key + ", rows=" + rows + "}";
    }
}
