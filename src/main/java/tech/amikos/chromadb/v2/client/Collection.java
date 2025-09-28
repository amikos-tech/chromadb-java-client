package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Collection interface representing a vector collection in ChromaDB.
 * Different implementations can provide server-specific or cloud-specific behavior.
 */
public interface Collection {

    // Metadata accessors
    UUID getId();
    String getName();
    String getTenant();
    String getDatabase();
    Map<String, Object> getMetadata();
    Integer getDimension();
    CollectionConfiguration getConfiguration();
    String getResourceName();

    // Count operation
    int count();

    // Query operations
    QueryBuilder query();
    QueryResponse query(Consumer<QueryRequest.Builder> configurator);

    // Get operations
    GetBuilder get();
    GetResponse get(Consumer<GetRequest.Builder> configurator);

    // Add operations
    AddBuilder add();
    void add(Consumer<AddRecordsRequest.Builder> configurator);

    // Update operations
    UpdateBuilder update();
    void update(Consumer<UpdateRecordsRequest.Builder> configurator);

    // Upsert operations
    UpsertBuilder upsert();
    void upsert(Consumer<AddRecordsRequest.Builder> configurator);

    // Delete operations
    DeleteBuilder delete();
    void delete(Consumer<DeleteRecordsRequest.Builder> configurator);

    // Fluent builder interfaces
    interface QueryBuilder {
        QueryBuilder queryEmbeddings(List<List<Float>> embeddings);
        QueryBuilder nResults(int nResults);
        QueryBuilder where(Where where);
        QueryBuilder whereDocument(WhereDocument whereDocument);
        QueryBuilder include(Include... include);
        QueryResponse execute();
    }

    interface GetBuilder {
        GetBuilder ids(List<String> ids);
        GetBuilder where(Where where);
        GetBuilder whereDocument(WhereDocument whereDocument);
        GetBuilder include(Include... include);
        GetBuilder limit(int limit);
        GetBuilder offset(int offset);
        GetResponse execute();
    }

    interface AddBuilder {
        AddBuilder ids(List<String> ids);
        AddBuilder embeddings(List<List<Float>> embeddings);
        AddBuilder documents(List<String> documents);
        AddBuilder metadatas(List<Map<String, Object>> metadatas);
        AddBuilder uris(List<String> uris);
        void execute();
    }

    interface UpdateBuilder {
        UpdateBuilder ids(List<String> ids);
        UpdateBuilder embeddings(List<List<Float>> embeddings);
        UpdateBuilder documents(List<String> documents);
        UpdateBuilder metadatas(List<Map<String, Object>> metadatas);
        UpdateBuilder uris(List<String> uris);
        void execute();
    }

    interface UpsertBuilder {
        UpsertBuilder ids(List<String> ids);
        UpsertBuilder embeddings(List<List<Float>> embeddings);
        UpsertBuilder documents(List<String> documents);
        UpsertBuilder metadatas(List<Map<String, Object>> metadatas);
        UpsertBuilder uris(List<String> uris);
        void execute();
    }

    interface DeleteBuilder {
        DeleteBuilder ids(List<String> ids);
        DeleteBuilder where(Where where);
        DeleteBuilder whereDocument(WhereDocument whereDocument);
        void execute();
    }
}