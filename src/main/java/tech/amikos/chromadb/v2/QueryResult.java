package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/**
 * Result from a query operation.
 *
 * <p>Each outer list is indexed per query input (text or embedding). Fields omitted via
 * {@link Include} may be {@code null}.</p>
 */
public interface QueryResult {

    /** Always present. */
    List<List<String>> getIds();

    /** Present when {@link Include#DOCUMENTS} is requested; otherwise may be {@code null}. */
    List<List<String>> getDocuments();

    /** Present when {@link Include#METADATAS} is requested; otherwise may be {@code null}. */
    List<List<Map<String, Object>>> getMetadatas();

    /** Present when {@link Include#EMBEDDINGS} is requested; otherwise may be {@code null}. */
    List<List<float[]>> getEmbeddings();

    /** Present when {@link Include#DISTANCES} is requested; otherwise may be {@code null}. */
    List<List<Float>> getDistances();

    /** Present when {@link Include#URIS} is requested; otherwise may be {@code null}. */
    List<List<String>> getUris();
}
