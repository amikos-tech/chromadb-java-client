package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.util.Arrays;
import java.util.List;

public interface EmbeddingFunction {

    /**
     * Embeds a single query string.
     */
    Embedding embedQuery(String query) throws EFException;

    /**
     * Embeds document texts for storage/indexing.
     */
    List<Embedding> embedDocuments(List<String> documents) throws EFException;

    /**
     * Embeds document texts for storage/indexing.
     */
    List<Embedding> embedDocuments(String[] documents) throws EFException;

    /**
     * Embeds query texts for search/ranking. Some providers expose query-specific models
     * or API input types; implementations may override this behavior.
     *
     * <p>Default behavior delegates to {@link #embedDocuments(List)} for backward compatibility.</p>
     */
    default List<Embedding> embedQueries(List<String> queries) throws EFException {
        return embedDocuments(queries);
    }

    /**
     * Embeds query texts for search/ranking. Default behavior delegates to
     * {@link #embedQueries(List)} for backward compatibility.
     */
    default List<Embedding> embedQueries(String[] queries) throws EFException {
        return embedQueries(Arrays.asList(queries));
    }
}
