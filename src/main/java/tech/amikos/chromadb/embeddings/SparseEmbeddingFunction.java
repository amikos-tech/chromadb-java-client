package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.v2.SparseVector;

import java.util.List;

/**
 * Embedding function interface for sparse vector representations (e.g., BM25, SPLADE).
 *
 * <p>This is a separate interface from {@link EmbeddingFunction} because sparse embeddings
 * produce {@link SparseVector} (integer indices + float values) rather than dense
 * {@link tech.amikos.chromadb.Embedding} arrays.</p>
 */
public interface SparseEmbeddingFunction {

    /**
     * Embeds a single query string into a sparse vector.
     *
     * @param query the query text
     * @return a sparse vector representation
     * @throws EFException if embedding fails
     */
    SparseVector embedQuery(String query) throws EFException;

    /**
     * Embeds multiple documents into sparse vectors.
     *
     * @param documents the document texts
     * @return a list of sparse vector representations, one per document
     * @throws EFException if embedding fails
     */
    List<SparseVector> embedDocuments(List<String> documents) throws EFException;
}
