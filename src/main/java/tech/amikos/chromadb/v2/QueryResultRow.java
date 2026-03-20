package tech.amikos.chromadb.v2;

/**
 * A result row from a query operation, extending {@link ResultRow} with a distance field.
 *
 * <p>{@link #getDistance()} returns {@code null} when {@link Include#DISTANCES} was not included
 * in the query request.
 */
public interface QueryResultRow extends ResultRow {

    /**
     * Returns the distance from the query embedding, or {@code null} if
     * {@link Include#DISTANCES} was not included.
     */
    Float getDistance();
}
