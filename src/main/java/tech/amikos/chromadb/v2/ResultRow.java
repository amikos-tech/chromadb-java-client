package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Represents a single result row from a get or query operation.
 *
 * <p>Fields are {@code null} when the corresponding {@link Include} value was not specified in the
 * request. No {@code Optional} wrappers are used — callers should check for {@code null} directly.
 */
public interface ResultRow {

    /**
     * Returns the record ID, or {@code null} if not included.
     */
    String getId();

    /**
     * Returns the document text, or {@code null} if {@link Include#DOCUMENTS} was not included.
     */
    String getDocument();

    /**
     * Returns an unmodifiable metadata map, or {@code null} if {@link Include#METADATAS} was not
     * included.
     */
    Map<String, Object> getMetadata();

    /**
     * Returns a defensive copy of the embedding array, or {@code null} if
     * {@link Include#EMBEDDINGS} was not included.
     */
    float[] getEmbedding();

    /**
     * Returns the URI, or {@code null} if {@link Include#URIS} was not included.
     */
    String getUri();
}
