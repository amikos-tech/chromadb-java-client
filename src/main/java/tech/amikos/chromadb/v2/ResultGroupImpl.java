package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Package-private implementation of {@link ResultGroup} backed by an unmodifiable list.
 *
 * <p>{@link #get(int)} delegates to the underlying list, which throws
 * {@link IndexOutOfBoundsException} for invalid indices per the standard {@link List} contract.
 *
 * @param <R> the row type, must extend {@link ResultRow}
 */
final class ResultGroupImpl<R extends ResultRow> implements ResultGroup<R> {

    private final List<R> rows;

    ResultGroupImpl(List<R> rows) {
        this.rows = Collections.unmodifiableList(new ArrayList<R>(rows));
    }

    @Override
    public R get(int index) {
        return rows.get(index);
    }

    @Override
    public int size() {
        return rows.size();
    }

    @Override
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public Iterator<R> iterator() {
        return rows.iterator();
    }

    @Override
    public Stream<R> stream() {
        return rows.stream();
    }

    @Override
    public List<R> toList() {
        return rows;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResultGroupImpl)) return false;
        ResultGroupImpl<?> other = (ResultGroupImpl<?>) obj;
        return rows.equals(other.rows);
    }

    @Override
    public int hashCode() {
        return rows.hashCode();
    }

    @Override
    public String toString() {
        return "ResultGroup" + rows.toString();
    }
}
