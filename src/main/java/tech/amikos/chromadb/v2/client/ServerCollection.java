package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.http.HttpClient;
import tech.amikos.chromadb.v2.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server implementation of Collection for self-hosted Chroma instances.
 */
public class ServerCollection implements Collection {
    private final CollectionModel model;
    private final HttpClient httpClient;

    public ServerCollection(CollectionModel model, HttpClient httpClient) {
        this.model = model;
        this.httpClient = httpClient;
    }

    @Override
    public UUID getId() {
        return model.getId();
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public String getTenant() {
        return model.getTenant();
    }

    @Override
    public String getDatabase() {
        return model.getDatabase();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return model.getMetadata();
    }

    @Override
    public Integer getDimension() {
        return model.getDimension();
    }

    @Override
    public CollectionConfiguration getConfiguration() {
        return model.getConfiguration();
    }

    @Override
    public String getResourceName() {
        return model.getResourceName();
    }

    private String basePath() {
        return String.format("/api/v1/collections/%s", getId());
    }

    @Override
    public int count() {
        return httpClient.get(basePath() + "/count", Integer.class);
    }

    @Override
    public QueryBuilder query() {
        return new ServerQueryBuilder();
    }

    @Override
    public QueryResponse query(Consumer<QueryRequest.Builder> configurator) {
        QueryRequest.Builder builder = QueryRequest.builder();
        configurator.accept(builder);
        QueryRequest request = builder.build();
        return httpClient.post(basePath() + "/query", request, QueryResponse.class);
    }

    @Override
    public GetBuilder get() {
        return new ServerGetBuilder();
    }

    @Override
    public GetResponse get(Consumer<GetRequest.Builder> configurator) {
        GetRequest.Builder builder = GetRequest.builder();
        if (configurator != null) {
            configurator.accept(builder);
        }
        GetRequest request = builder.build();
        return httpClient.post(basePath() + "/get", request, GetResponse.class);
    }

    @Override
    public AddBuilder add() {
        return new ServerAddBuilder();
    }

    @Override
    public void add(Consumer<AddRecordsRequest.Builder> configurator) {
        AddRecordsRequest.Builder builder = AddRecordsRequest.builder();
        configurator.accept(builder);
        AddRecordsRequest request = builder.build();
        httpClient.post(basePath() + "/add", request, Void.class);
    }

    @Override
    public UpdateBuilder update() {
        return new ServerUpdateBuilder();
    }

    @Override
    public void update(Consumer<UpdateRecordsRequest.Builder> configurator) {
        UpdateRecordsRequest.Builder builder = UpdateRecordsRequest.builder();
        configurator.accept(builder);
        UpdateRecordsRequest request = builder.build();
        httpClient.post(basePath() + "/update", request, Void.class);
    }

    @Override
    public UpsertBuilder upsert() {
        return new ServerUpsertBuilder();
    }

    @Override
    public void upsert(Consumer<AddRecordsRequest.Builder> configurator) {
        AddRecordsRequest.Builder builder = AddRecordsRequest.builder();
        configurator.accept(builder);
        AddRecordsRequest request = builder.build();
        httpClient.post(basePath() + "/upsert", request, Void.class);
    }

    @Override
    public DeleteBuilder delete() {
        return new ServerDeleteBuilder();
    }

    @Override
    public void delete(Consumer<DeleteRecordsRequest.Builder> configurator) {
        DeleteRecordsRequest.Builder builder = DeleteRecordsRequest.builder();
        if (configurator != null) {
            configurator.accept(builder);
        }
        DeleteRecordsRequest request = builder.build();
        httpClient.post(basePath() + "/delete", request, Void.class);
    }

    private class ServerQueryBuilder implements QueryBuilder {
        private final QueryRequest.Builder builder = QueryRequest.builder();

        @Override
        public QueryBuilder queryEmbeddings(List<List<Float>> embeddings) {
            builder.queryEmbeddings(embeddings);
            return this;
        }

        @Override
        public QueryBuilder nResults(int nResults) {
            builder.nResults(nResults);
            return this;
        }

        @Override
        public QueryBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        @Override
        public QueryBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        @Override
        public QueryBuilder include(Include... include) {
            builder.include(include);
            return this;
        }

        @Override
        public QueryResponse execute() {
            QueryRequest request = builder.build();
            return httpClient.post(basePath() + "/query", request, QueryResponse.class);
        }
    }

    private class ServerGetBuilder implements GetBuilder {
        private final GetRequest.Builder builder = GetRequest.builder();

        @Override
        public GetBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        @Override
        public GetBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        @Override
        public GetBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        @Override
        public GetBuilder include(Include... include) {
            builder.include(include);
            return this;
        }

        @Override
        public GetBuilder limit(int limit) {
            builder.limit(limit);
            return this;
        }

        @Override
        public GetBuilder offset(int offset) {
            builder.offset(offset);
            return this;
        }

        @Override
        public GetResponse execute() {
            GetRequest request = builder.build();
            return httpClient.post(basePath() + "/get", request, GetResponse.class);
        }
    }

    private class ServerAddBuilder implements AddBuilder {
        private final AddRecordsRequest.Builder builder = AddRecordsRequest.builder();

        @Override
        public AddBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        @Override
        public AddBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        @Override
        public AddBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        @Override
        public AddBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        @Override
        public AddBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        @Override
        public void execute() {
            AddRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/add", request, Void.class);
        }
    }

    private class ServerUpdateBuilder implements UpdateBuilder {
        private final UpdateRecordsRequest.Builder builder = UpdateRecordsRequest.builder();

        @Override
        public UpdateBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        @Override
        public UpdateBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        @Override
        public UpdateBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        @Override
        public UpdateBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        @Override
        public UpdateBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        @Override
        public void execute() {
            UpdateRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/update", request, Void.class);
        }
    }

    private class ServerUpsertBuilder implements UpsertBuilder {
        private final AddRecordsRequest.Builder builder = AddRecordsRequest.builder();

        @Override
        public UpsertBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        @Override
        public UpsertBuilder embeddings(List<List<Float>> embeddings) {
            builder.embeddings(embeddings);
            return this;
        }

        @Override
        public UpsertBuilder documents(List<String> documents) {
            builder.documents(documents);
            return this;
        }

        @Override
        public UpsertBuilder metadatas(List<Map<String, Object>> metadatas) {
            builder.metadatas(metadatas);
            return this;
        }

        @Override
        public UpsertBuilder uris(List<String> uris) {
            builder.uris(uris);
            return this;
        }

        @Override
        public void execute() {
            AddRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/upsert", request, Void.class);
        }
    }

    private class ServerDeleteBuilder implements DeleteBuilder {
        private final DeleteRecordsRequest.Builder builder = DeleteRecordsRequest.builder();

        @Override
        public DeleteBuilder ids(List<String> ids) {
            builder.ids(ids);
            return this;
        }

        @Override
        public DeleteBuilder where(Where where) {
            builder.where(where);
            return this;
        }

        @Override
        public DeleteBuilder whereDocument(WhereDocument whereDocument) {
            builder.whereDocument(whereDocument);
            return this;
        }

        @Override
        public void execute() {
            DeleteRecordsRequest request = builder.build();
            httpClient.post(basePath() + "/delete", request, Void.class);
        }
    }
}