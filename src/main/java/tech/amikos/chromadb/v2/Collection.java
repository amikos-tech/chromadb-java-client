package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/**
 * A ChromaDB collection. Provides record operations via builders.
 *
 * <pre>{@code
 * // Add records
 * collection.add()
 *     .ids("id1", "id2")
 *     .documents("doc1", "doc2")
 *     .execute();
 *
 * // Query
 * QueryResult result = collection.query()
 *     .queryEmbeddings(new float[]{0.12f, 0.34f, 0.56f})
 *     .nResults(10)
 *     .include(Include.DOCUMENTS, Include.DISTANCES)
 *     .execute();
 * }</pre>
 *
 * <p>All record operations may throw unchecked exceptions from the {@link ChromaException} hierarchy.</p>
 */
public interface Collection {

    String getId();

    String getName();

    Tenant getTenant();

    Database getDatabase();

    Map<String, Object> getMetadata();

    Integer getDimension();

    CollectionConfiguration getConfiguration();

    /**
     * Returns the typed collection schema when available.
     *
     * <p>When both schema sources are present in server payloads, top-level {@code schema}
     * takes precedence over {@code configuration.schema}. After initial resolution, local
     * configuration updates only backfill schema when no schema has been resolved yet.</p>
     */
    default Schema getSchema() {
        return null;
    }

    // --- Record operations ---

    AddBuilder add();

    QueryBuilder query();

    GetBuilder get();

    UpdateBuilder update();

    UpsertBuilder upsert();

    DeleteBuilder delete();

    /** @throws ChromaServerException on server errors */
    int count();

    // --- Modification ---

    /** @throws ChromaNotFoundException if the collection no longer exists */
    void modifyName(String newName);

    /**
     * Sends a partial metadata update to the server and applies the same merge to this local
     * collection snapshot (last write wins on key collisions).
     *
     * @param metadata non-null map of metadata keys to merge
     * @throws NullPointerException if {@code metadata} is null
     * @throws ChromaNotFoundException if the collection no longer exists
     */
    void modifyMetadata(Map<String, Object> metadata);

    /**
     * Updates mutable runtime indexing parameters for this collection.
     *
     * <p>Exactly one configuration group must be provided: HNSW or SPANN.</p>
     *
     * @param config non-null runtime configuration update
     * @throws NullPointerException if {@code config} is null
     * @throws IllegalArgumentException if {@code config} is empty, mixes HNSW and SPANN fields,
     *                                  or conflicts with the currently active index group
     * @throws ChromaNotFoundException if the collection no longer exists
     */
    void modifyConfiguration(UpdateCollectionConfiguration config);

    // --- Builders ---

    interface AddBuilder {
        AddBuilder ids(String... ids);
        AddBuilder ids(List<String> ids);
        AddBuilder embeddings(float[]... embeddings);
        AddBuilder embeddings(List<float[]> embeddings);
        AddBuilder documents(String... documents);
        AddBuilder documents(List<String> documents);
        AddBuilder metadatas(List<Map<String, Object>> metadatas);
        AddBuilder uris(String... uris);
        AddBuilder uris(List<String> uris);
        /** @throws ChromaBadRequestException if the input is invalid */
        void execute();
    }

    interface QueryBuilder {
        /**
         * Queries by raw text. The client resolves an embedding function from:
         * explicit runtime options, {@code configuration.embedding_function},
         * top-level schema {@code #embedding} vector index embedding function, then
         * {@code configuration.schema} {@code #embedding} vector index embedding function.
         *
         * <p>For text queries, the resolved embedding function uses
         * {@code EmbeddingFunction.embedQueries(...)}. Providers may implement
         * query-specific API primitives that differ from document embedding behavior.</p>
         *
         * @throws NullPointerException if {@code texts} is null
         * @throws IllegalArgumentException if empty text input is provided or any element is null
         */
        QueryBuilder queryTexts(String... texts);

        /**
         * Queries by raw text. Uses the same embedding-function resolution order as
         * {@link #queryTexts(String...)}. Rejects null/empty input and cannot be mixed
         * with {@link #queryEmbeddings(float[]...)} or {@link #queryEmbeddings(List)}.
         *
         * <p>For text queries, the resolved embedding function uses
         * {@code EmbeddingFunction.embedQueries(...)}. Providers may implement
         * query-specific API primitives that differ from document embedding behavior.</p>
         *
         * @throws NullPointerException if {@code texts} is null
         * @throws IllegalArgumentException if empty text input is provided or any element is null
         */
        QueryBuilder queryTexts(List<String> texts);
        QueryBuilder queryEmbeddings(float[]... embeddings);
        QueryBuilder queryEmbeddings(List<float[]> embeddings);
        QueryBuilder nResults(int nResults);
        QueryBuilder where(Where where);
        QueryBuilder whereDocument(WhereDocument whereDocument);
        QueryBuilder include(Include... include);
        /**
         * @throws IllegalArgumentException if {@code where}/{@code whereDocument} return null from {@code toMap()}
         * @throws ChromaBadRequestException if the query is invalid
         * @throws ChromaException if text-query embedding resolution or embedding generation fails
         */
        QueryResult execute();
    }

    interface GetBuilder {
        GetBuilder ids(String... ids);
        GetBuilder ids(List<String> ids);
        GetBuilder where(Where where);
        GetBuilder whereDocument(WhereDocument whereDocument);
        GetBuilder include(Include... include);
        GetBuilder limit(int limit);
        GetBuilder offset(int offset);
        /**
         * @throws IllegalArgumentException if {@code where}/{@code whereDocument} return null from {@code toMap()}
         * @throws ChromaBadRequestException if the request is invalid
         */
        GetResult execute();
    }

    interface UpdateBuilder {
        UpdateBuilder ids(String... ids);
        UpdateBuilder ids(List<String> ids);
        UpdateBuilder embeddings(float[]... embeddings);
        UpdateBuilder embeddings(List<float[]> embeddings);
        UpdateBuilder documents(String... documents);
        UpdateBuilder documents(List<String> documents);
        UpdateBuilder metadatas(List<Map<String, Object>> metadatas);
        /** @throws ChromaBadRequestException if the input is invalid */
        void execute();
    }

    interface UpsertBuilder {
        UpsertBuilder ids(String... ids);
        UpsertBuilder ids(List<String> ids);
        UpsertBuilder embeddings(float[]... embeddings);
        UpsertBuilder embeddings(List<float[]> embeddings);
        UpsertBuilder documents(String... documents);
        UpsertBuilder documents(List<String> documents);
        UpsertBuilder metadatas(List<Map<String, Object>> metadatas);
        UpsertBuilder uris(String... uris);
        UpsertBuilder uris(List<String> uris);
        /** @throws ChromaBadRequestException if the input is invalid */
        void execute();
    }

    interface DeleteBuilder {
        DeleteBuilder ids(String... ids);
        DeleteBuilder ids(List<String> ids);
        /**
         * Applies a {@link Where} filter to restrict which records are deleted.
         *
         * <p>Validation is performed at {@link #execute()} time.</p>
         */
        DeleteBuilder where(Where where);
        DeleteBuilder whereDocument(WhereDocument whereDocument);
        /**
         * @throws IllegalArgumentException if no criteria are provided or if {@code where}/{@code whereDocument}
         *                                  return null from {@code toMap()}
         * @throws ChromaBadRequestException if the filter is invalid
         */
        void execute();
    }
}
