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
 *     .where(Where.eq("type", "article"))
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

    /** @throws ChromaNotFoundException if the collection no longer exists */
    void modifyMetadata(Map<String, Object> metadata);

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
        QueryBuilder queryTexts(String... texts);
        QueryBuilder queryTexts(List<String> texts);
        QueryBuilder queryEmbeddings(float[]... embeddings);
        QueryBuilder queryEmbeddings(List<float[]> embeddings);
        QueryBuilder nResults(int nResults);
        QueryBuilder where(Where where);
        QueryBuilder whereDocument(WhereDocument whereDocument);
        QueryBuilder include(Include... include);
        /** @throws ChromaBadRequestException if the query is invalid */
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
        /** @throws ChromaBadRequestException if the request is invalid */
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
        DeleteBuilder where(Where where);
        DeleteBuilder whereDocument(WhereDocument whereDocument);
        /** @throws ChromaBadRequestException if the filter is invalid */
        void execute();
    }
}
