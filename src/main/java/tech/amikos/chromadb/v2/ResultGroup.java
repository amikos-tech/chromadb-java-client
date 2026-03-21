package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.stream.Stream;

/**
 * An ordered, iterable group of result rows.
 *
 * <p>Instances are immutable. All accessor methods are safe to call from multiple threads.
 *
 * @param <R> the row type, must extend {@link ResultRow}
 */
public interface ResultGroup<R extends ResultRow> extends Iterable<R> {

    /**
     * Returns the row at the given index.
     *
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    R get(int index);

    /**
     * Returns the number of rows in this group.
     */
    int size();

    /**
     * Returns {@code true} if this group contains no rows.
     */
    boolean isEmpty();

    /**
     * Returns a sequential {@link Stream} of the rows in this group.
     */
    Stream<R> stream();

    /**
     * Returns an unmodifiable {@link List} view of the rows in this group.
     */
    List<R> toList();
}
