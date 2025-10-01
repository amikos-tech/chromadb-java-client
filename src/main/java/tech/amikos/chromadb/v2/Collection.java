package tech.amikos.chromadb.v2;

import tech.amikos.chromadb.v2.HttpClient;
import tech.amikos.chromadb.v2.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server implementation of Collection for self-hosted Chroma instances.
 */
/**
 * Concrete Collection class representing a ChromaDB collection.
 * Uses builder pattern for all complex operations, following radical simplicity principles.
 */
public class Collection {
    private final CollectionModel model;
    private final HttpClient httpClient;

    public Collection(HttpClient httpClient, CollectionModel model) {
        this.model = model;
        this.httpClient = httpClient;
    }

    public UUID getId() {
        return model.getId();
    }

    public String getName() {
        return model.getName();
    }

    public String getTenant() {
        return model.getTenant();
    }

    public String getDatabase() {
        return model.getDatabase();
    }

    public Map<String, Object> getMetadata() {
        return model.getMetadata();
    }

    public Integer getDimension() {
        return model.getDimension();
    }

    public CollectionConfiguration getConfiguration() {
        return model.getConfiguration();
    }

    public String getResourceName() {
        return model.getResourceName();
    }

    private String basePath() {
        return String.format("/api/v2/tenants/%s/databases/%s/collections/%s",
                getTenant(), getDatabase(), getId());
    }

    public int count() {
        return httpClient.get(basePath() + "/count", Integer.class);
    }

    public QueryBuilder query() {
        return new QueryBuilder();
    }


    public GetBuilder get() {
        return new GetBuilder();
    }


    public AddBuilder add() {
        return new AddBuilder();
    }


    public UpdateBuilder update() {
        return new UpdateBuilder();
    }


    public UpsertBuilder upsert() {
        return new UpsertBuilder();
    }


    public DeleteBuilder delete() {
        return new DeleteBuilder();
    }


    public class QueryBuilder {
        private final QueryRequest.Builder builder = QueryRequest.builder();

        public QueryBuilder queryEmbeddings(List<List<Float>> embeddings) {
            builder.queryEmbeddings(embeddings);
            return this;
        }

        public QueryBuilder nResults(int nResults) {
            builder.nResults(nResults);
            return this;
        }

        public QueryBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        public QueryBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        public QueryBuilder include(Include... include) {
            builder.include(include);
            return this;
        }

        public QueryResponse execute() {
            QueryRequest request = builder.build();
            return httpClient.post(basePath() + "/query", request, QueryResponse.class);
        }
    }

    public class GetBuilder {
        private final GetRequest.Builder builder = GetRequest.builder();

        public GetBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        public GetBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        public GetBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        public GetBuilder include(Include... include) {
            builder.include(include);
            return this;
        }

        public GetBuilder limit(int limit) {
            builder.limit(limit);
            return this;
        }

        public GetBuilder offset(int offset) {
            builder.offset(offset);
            return this;
        }

        public GetResponse execute() {
            GetRequest request = builder.build();
            return httpClient.post(basePath() + "/get", request, GetResponse.class);
        }
    }

    public class AddBuilder {
        private final AddRecordsRequest.Builder builder = AddRecordsRequest.builder();

        public AddBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        public AddBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        public AddBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        public AddBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        public AddBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        public void execute() {
            AddRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/add", request, Void.class);
        }
    }

    public class UpdateBuilder {
        private final UpdateRecordsRequest.Builder builder = UpdateRecordsRequest.builder();

        public UpdateBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        public UpdateBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        public UpdateBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        public UpdateBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        public UpdateBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        public void execute() {
            UpdateRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/update", request, Void.class);
        }
    }

    public class UpsertBuilder {
        private final AddRecordsRequest.Builder builder = AddRecordsRequest.builder();

        public UpsertBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        public UpsertBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        public UpsertBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        public UpsertBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        public UpsertBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        public void execute() {
            AddRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/upsert", request, Void.class);
        }
    }

    public class DeleteBuilder {
        private final DeleteRecordsRequest.Builder builder = DeleteRecordsRequest.builder();

        public DeleteBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        public DeleteBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        public DeleteBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        public void execute() {
            DeleteRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/delete", request, Void.class);
        }
    }
}