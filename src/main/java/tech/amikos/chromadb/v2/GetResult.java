package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

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
}
