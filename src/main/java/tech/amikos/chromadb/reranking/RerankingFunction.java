package tech.amikos.chromadb.reranking;

import tech.amikos.chromadb.EFException;

import java.util.List;

/**
 * Interface for reranking documents by relevance to a query.
 *
 * <p>Implementations call external reranking services (e.g. Cohere, Jina)
 * to score and sort documents by relevance.</p>
 */
public interface RerankingFunction {

    /**
     * Reranks the given documents by relevance to the query.
     *
     * @param query     the query string
     * @param documents the documents to rerank
     * @return results sorted by descending relevance score
     * @throws EFException if the reranking call fails
     */
    List<RerankResult> rerank(String query, List<String> documents) throws EFException;
}
