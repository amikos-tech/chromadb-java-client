package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Represents a single result row from a get, query, or search operation.
 *
 * <p>Fields are {@code null} when the corresponding projection was not requested (e.g.,
 * {@link Include} for get/query, {@link Select} for search). No {@code Optional} wrappers
 * are used — callers should check for {@code null} directly.
 */
public interface ResultRow {

    /**
     * Returns the record ID, or {@code null} if not included.
     */
    String getId();

    /**
     * Returns the document text, or {@code null} if document projection was not requested.
     */
    String getDocument();

    /**
     * Returns an unmodifiable metadata map, or {@code null} if metadata projection was not
     * requested.
     */
    Map<String, Object> getMetadata();

    /**
     * Returns a defensive copy of the embedding array, or {@code null} if embedding projection
     * was not requested.
     */
    float[] getEmbedding();

    /**
     * Returns the URI, or {@code null} if URI projection was not requested.
     */
    String getUri();
}
