package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Result from a get operation. Flat lists of matching records.
 *
 * <p>Fields omitted via {@link Include} may be {@code null}.</p>
 */
public interface GetResult {

    /** Always present. */
    List<String> getIds();

    /** Present when {@link Include#DOCUMENTS} is requested; otherwise may be {@code null}. */
    List<String> getDocuments();

    /** Present when {@link Include#METADATAS} is requested; otherwise may be {@code null}. */
    List<Map<String, Object>> getMetadatas();

    /** Present when {@link Include#EMBEDDINGS} is requested; otherwise may be {@code null}. */
    List<float[]> getEmbeddings();

    /** Present when {@link Include#URIS} is requested; otherwise may be {@code null}. */
    List<String> getUris();

    /**
     * Returns all results as a row-oriented group.
     *
     * @return group of rows, one per matching record
     */
    ResultGroup<ResultRow> rows();

    /**
     * Returns a sequential {@link Stream} of the result rows.
     *
     * <p>Equivalent to {@code rows().stream()}.
     */
    Stream<ResultRow> stream();
}
