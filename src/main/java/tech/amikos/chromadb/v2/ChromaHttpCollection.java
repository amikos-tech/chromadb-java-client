package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Package-private {@link Collection} implementation backed by HTTP transport.
 *
 * <p>Mutable properties use {@code volatile} fields so getter calls observe updates across
 * threads. This class does not provide full transactional thread-safety for concurrent
 * mutations; concurrent writes are last-write-wins. Metadata merge updates are synchronized
 * locally to avoid lost updates from concurrent read-modify-write sequences.</p>
 */
final class ChromaHttpCollection implements Collection {

    private final ChromaApiClient apiClient;
    private final String id;
    private final Tenant tenant;
    private final Database database;

    private volatile String name;
    private volatile Map<String, Object> metadata;
    private volatile Integer dimension;
    private volatile CollectionConfiguration configuration;

    private ChromaHttpCollection(ChromaApiClient apiClient, String id, String name,
                                 Tenant tenant, Database database,
                                 Map<String, Object> metadata, Integer dimension,
                                 CollectionConfiguration configuration) {
        this.apiClient = apiClient;
        this.id = id;
        this.name = name;
        this.tenant = tenant;
        this.database = database;
        this.metadata = copyMetadata(metadata);
        this.dimension = dimension;
        this.configuration = configuration;
    }

    static ChromaHttpCollection from(ChromaDtos.CollectionResponse dto,
                                     ChromaApiClient apiClient,
                                     Tenant tenant, Database database) {
        if (dto == null) {
            throw new ChromaDeserializationException(
                    "Server returned an empty collection payload",
                    200
            );
        }
        Objects.requireNonNull(apiClient, "apiClient");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(database, "database");
        return new ChromaHttpCollection(
                apiClient,
                requireNonBlankField("collection.id", dto.id),
                requireNonBlankField("collection.name", dto.name),
                tenant,
                database,
                dto.metadata,
                dto.dimension,
                ChromaDtos.parseConfiguration(dto.configurationJson)
        );
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : Collections.unmodifiableMap(metadata);
    }

    @Override
    public Integer getDimension() {
        return dimension;
    }

    @Override
    public CollectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public int count() {
        String path = ChromaApiPaths.collectionCount(tenant.getName(), database.getName(), id);
        return apiClient.get(path, Integer.class);
    }

    @Override
    public void modifyName(String newName) {
        String normalizedName = requireNonBlankArgument("newName", newName);
        String path = ChromaApiPaths.collectionById(tenant.getName(), database.getName(), id);
        apiClient.put(path, new ChromaDtos.UpdateCollectionRequest(normalizedName, null, null));
        this.name = normalizedName;
    }

    @Override
    public void modifyMetadata(Map<String, Object> metadata) {
        Objects.requireNonNull(metadata, "metadata");
        String path = ChromaApiPaths.collectionById(tenant.getName(), database.getName(), id);
        apiClient.put(path, new ChromaDtos.UpdateCollectionRequest(null, metadata, null));
        mergeLocalMetadata(metadata);
    }

    @Override
    public void modifyConfiguration(UpdateCollectionConfiguration config) {
        Objects.requireNonNull(config, "config");
        config.validate();
        Map<String, Object> updatePayload = ChromaDtos.toUpdateConfigurationMap(config);
        if (updatePayload == null) {
            throw new IllegalStateException("Validated configuration update must serialize to non-null payload");
        }
        String path = ChromaApiPaths.collectionById(tenant.getName(), database.getName(), id);
        apiClient.put(path, new ChromaDtos.UpdateCollectionRequest(
                null,
                null,
                updatePayload
        ));
        mergeLocalConfiguration(config);
    }

    private void mergeLocalMetadata(Map<String, Object> metadataUpdate) {
        Map<String, Object> merged = new LinkedHashMap<String, Object>();
        synchronized (this) {
            if (this.metadata != null) {
                merged.putAll(this.metadata);
            }
            merged.putAll(copyMetadata(metadataUpdate));
            this.metadata = merged;
        }
    }

    private synchronized void mergeLocalConfiguration(UpdateCollectionConfiguration update) {
        CollectionConfiguration.Builder builder = CollectionConfiguration.builder();
        if (this.configuration != null) {
            if (this.configuration.getSpace() != null) {
                builder.space(this.configuration.getSpace());
            }
            if (this.configuration.getHnswM() != null) {
                builder.hnswM(this.configuration.getHnswM().intValue());
            }
            if (this.configuration.getHnswConstructionEf() != null) {
                builder.hnswConstructionEf(this.configuration.getHnswConstructionEf().intValue());
            }
            if (this.configuration.getHnswSearchEf() != null) {
                builder.hnswSearchEf(this.configuration.getHnswSearchEf().intValue());
            }
            if (this.configuration.getHnswNumThreads() != null) {
                builder.hnswNumThreads(this.configuration.getHnswNumThreads().intValue());
            }
            if (this.configuration.getHnswBatchSize() != null) {
                builder.hnswBatchSize(this.configuration.getHnswBatchSize().intValue());
            }
            if (this.configuration.getHnswSyncThreshold() != null) {
                builder.hnswSyncThreshold(this.configuration.getHnswSyncThreshold().intValue());
            }
            if (this.configuration.getHnswResizeFactor() != null) {
                builder.hnswResizeFactor(this.configuration.getHnswResizeFactor().doubleValue());
            }
            if (this.configuration.getSpannSearchNprobe() != null) {
                builder.spannSearchNprobe(this.configuration.getSpannSearchNprobe().intValue());
            }
            if (this.configuration.getSpannEfSearch() != null) {
                builder.spannEfSearch(this.configuration.getSpannEfSearch().intValue());
            }
        }
        if (update.getHnswSearchEf() != null) {
            builder.hnswSearchEf(update.getHnswSearchEf().intValue());
        }
        if (update.getHnswNumThreads() != null) {
            builder.hnswNumThreads(update.getHnswNumThreads().intValue());
        }
        if (update.getHnswBatchSize() != null) {
            builder.hnswBatchSize(update.getHnswBatchSize().intValue());
        }
        if (update.getHnswSyncThreshold() != null) {
            builder.hnswSyncThreshold(update.getHnswSyncThreshold().intValue());
        }
        if (update.getHnswResizeFactor() != null) {
            builder.hnswResizeFactor(update.getHnswResizeFactor().doubleValue());
        }
        if (update.getSpannSearchNprobe() != null) {
            builder.spannSearchNprobe(update.getSpannSearchNprobe().intValue());
        }
        if (update.getSpannEfSearch() != null) {
            builder.spannEfSearch(update.getSpannEfSearch().intValue());
        }
        this.configuration = builder.build();
    }

    @Override
    public AddBuilder add() {
        return new AddBuilderImpl();
    }

    @Override
    public QueryBuilder query() {
        return new QueryBuilderImpl();
    }

    @Override
    public GetBuilder get() {
        return new GetBuilderImpl();
    }

    @Override
    public UpdateBuilder update() {
        return new UpdateBuilderImpl();
    }

    @Override
    public UpsertBuilder upsert() {
        return new UpsertBuilderImpl();
    }

    @Override
    public DeleteBuilder delete() {
        return new DeleteBuilderImpl();
    }

    // --- Builder implementations ---

    private final class AddBuilderImpl implements AddBuilder {
        private List<String> ids;
        private List<float[]> embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<String> uris;

        @Override
        public AddBuilder ids(String... ids) {
            this.ids = Arrays.asList(ids);
            return this;
        }

        @Override
        public AddBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public AddBuilder embeddings(float[]... embeddings) {
            this.embeddings = Arrays.asList(embeddings);
            return this;
        }

        @Override
        public AddBuilder embeddings(List<float[]> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        @Override
        public AddBuilder documents(String... documents) {
            this.documents = Arrays.asList(documents);
            return this;
        }

        @Override
        public AddBuilder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        @Override
        public AddBuilder metadatas(List<Map<String, Object>> metadatas) {
            this.metadatas = metadatas;
            return this;
        }

        @Override
        public AddBuilder uris(String... uris) {
            this.uris = Arrays.asList(uris);
            return this;
        }

        @Override
        public AddBuilder uris(List<String> uris) {
            this.uris = uris;
            return this;
        }

        @Override
        public void execute() {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids must not be empty");
            }
            int idsSize = ids.size();
            validateSizeMatchesIds("embeddings", embeddings, idsSize);
            validateSizeMatchesIds("documents", documents, idsSize);
            validateSizeMatchesIds("metadatas", metadatas, idsSize);
            validateSizeMatchesIds("uris", uris, idsSize);
            String path = ChromaApiPaths.collectionAdd(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.AddRequest(
                    ids,
                    ChromaDtos.toFloatLists(embeddings),
                    documents,
                    metadatas,
                    uris
            ));
        }
    }

    private final class UpsertBuilderImpl implements UpsertBuilder {
        private List<String> ids;
        private List<float[]> embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<String> uris;

        @Override
        public UpsertBuilder ids(String... ids) {
            this.ids = Arrays.asList(ids);
            return this;
        }

        @Override
        public UpsertBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public UpsertBuilder embeddings(float[]... embeddings) {
            this.embeddings = Arrays.asList(embeddings);
            return this;
        }

        @Override
        public UpsertBuilder embeddings(List<float[]> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        @Override
        public UpsertBuilder documents(String... documents) {
            this.documents = Arrays.asList(documents);
            return this;
        }

        @Override
        public UpsertBuilder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        @Override
        public UpsertBuilder metadatas(List<Map<String, Object>> metadatas) {
            this.metadatas = metadatas;
            return this;
        }

        @Override
        public UpsertBuilder uris(String... uris) {
            this.uris = Arrays.asList(uris);
            return this;
        }

        @Override
        public UpsertBuilder uris(List<String> uris) {
            this.uris = uris;
            return this;
        }

        @Override
        public void execute() {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids must not be empty");
            }
            int idsSize = ids.size();
            validateSizeMatchesIds("embeddings", embeddings, idsSize);
            validateSizeMatchesIds("documents", documents, idsSize);
            validateSizeMatchesIds("metadatas", metadatas, idsSize);
            validateSizeMatchesIds("uris", uris, idsSize);
            String path = ChromaApiPaths.collectionUpsert(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.UpsertRequest(
                    ids,
                    ChromaDtos.toFloatLists(embeddings),
                    documents,
                    metadatas,
                    uris
            ));
        }
    }

    private final class QueryBuilderImpl implements QueryBuilder {
        private List<float[]> queryEmbeddings;
        private int nResults = 10;
        private Where where;
        private WhereDocument whereDocument;
        private List<Include> include;

        @Override
        public QueryBuilder queryTexts(String... texts) {
            throw new UnsupportedOperationException(
                    "queryTexts requires an embedding function, which is not yet supported");
        }

        @Override
        public QueryBuilder queryTexts(List<String> texts) {
            throw new UnsupportedOperationException(
                    "queryTexts requires an embedding function, which is not yet supported");
        }

        @Override
        public QueryBuilder queryEmbeddings(float[]... embeddings) {
            this.queryEmbeddings = Arrays.asList(embeddings);
            return this;
        }

        @Override
        public QueryBuilder queryEmbeddings(List<float[]> embeddings) {
            this.queryEmbeddings = embeddings;
            return this;
        }

        @Override
        public QueryBuilder nResults(int nResults) {
            if (nResults <= 0) {
                throw new IllegalArgumentException("nResults must be > 0");
            }
            this.nResults = nResults;
            return this;
        }

        @Override
        public QueryBuilder where(Where where) {
            this.where = where;
            return this;
        }

        @Override
        public QueryBuilder whereDocument(WhereDocument whereDocument) {
            this.whereDocument = whereDocument;
            return this;
        }

        @Override
        public QueryBuilder include(Include... include) {
            this.include = Arrays.asList(include);
            return this;
        }

        @Override
        public QueryResult execute() {
            if (queryEmbeddings == null || queryEmbeddings.isEmpty()) {
                throw new IllegalArgumentException("queryEmbeddings must be provided");
            }
            List<String> includeValues = null;
            if (include != null) {
                includeValues = new ArrayList<String>(include.size());
                for (Include inc : include) {
                    includeValues.add(inc.getValue());
                }
            }
            String path = ChromaApiPaths.collectionQuery(tenant.getName(), database.getName(), id);
            ChromaDtos.QueryResponse dto = apiClient.post(path, new ChromaDtos.QueryRequest(
                    ChromaDtos.toFloatLists(queryEmbeddings),
                    nResults,
                    where != null ? where.toMap() : null,
                    whereDocument != null ? whereDocument.toMap() : null,
                    includeValues
            ), ChromaDtos.QueryResponse.class);
            return QueryResultImpl.from(dto);
        }
    }

    private final class GetBuilderImpl implements GetBuilder {
        private List<String> ids;
        private Where where;
        private WhereDocument whereDocument;
        private List<Include> include;
        private Integer limit;
        private Integer offset;

        @Override
        public GetBuilder ids(String... ids) {
            this.ids = Arrays.asList(ids);
            return this;
        }

        @Override
        public GetBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public GetBuilder where(Where where) {
            this.where = where;
            return this;
        }

        @Override
        public GetBuilder whereDocument(WhereDocument whereDocument) {
            this.whereDocument = whereDocument;
            return this;
        }

        @Override
        public GetBuilder include(Include... include) {
            this.include = Arrays.asList(include);
            return this;
        }

        @Override
        public GetBuilder limit(int limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("limit must be >= 0");
            }
            this.limit = limit;
            return this;
        }

        @Override
        public GetBuilder offset(int offset) {
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be >= 0");
            }
            this.offset = offset;
            return this;
        }

        @Override
        public GetResult execute() {
            List<String> includeValues = null;
            if (include != null) {
                includeValues = new ArrayList<String>(include.size());
                for (Include inc : include) {
                    includeValues.add(inc.getValue());
                }
            }
            String path = ChromaApiPaths.collectionGet(tenant.getName(), database.getName(), id);
            ChromaDtos.GetResponse dto = apiClient.post(path, new ChromaDtos.GetRequest(
                    ids,
                    where != null ? where.toMap() : null,
                    whereDocument != null ? whereDocument.toMap() : null,
                    includeValues,
                    limit,
                    offset
            ), ChromaDtos.GetResponse.class);
            return GetResultImpl.from(dto);
        }
    }

    private final class UpdateBuilderImpl implements UpdateBuilder {
        private List<String> ids;
        private List<float[]> embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;

        @Override
        public UpdateBuilder ids(String... ids) {
            this.ids = Arrays.asList(ids);
            return this;
        }

        @Override
        public UpdateBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public UpdateBuilder embeddings(float[]... embeddings) {
            this.embeddings = Arrays.asList(embeddings);
            return this;
        }

        @Override
        public UpdateBuilder embeddings(List<float[]> embeddings) {
            this.embeddings = embeddings;
            return this;
        }

        @Override
        public UpdateBuilder documents(String... documents) {
            this.documents = Arrays.asList(documents);
            return this;
        }

        @Override
        public UpdateBuilder documents(List<String> documents) {
            this.documents = documents;
            return this;
        }

        @Override
        public UpdateBuilder metadatas(List<Map<String, Object>> metadatas) {
            this.metadatas = metadatas;
            return this;
        }

        @Override
        public void execute() {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("ids must not be empty");
            }
            int idsSize = ids.size();
            validateSizeMatchesIds("embeddings", embeddings, idsSize);
            validateSizeMatchesIds("documents", documents, idsSize);
            validateSizeMatchesIds("metadatas", metadatas, idsSize);
            String path = ChromaApiPaths.collectionUpdate(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.UpdateRequest(
                    ids,
                    ChromaDtos.toFloatLists(embeddings),
                    documents,
                    metadatas
            ));
        }
    }

    private final class DeleteBuilderImpl implements DeleteBuilder {
        private List<String> ids;
        private Where where;
        private WhereDocument whereDocument;

        @Override
        public DeleteBuilder ids(String... ids) {
            this.ids = Arrays.asList(ids);
            return this;
        }

        @Override
        public DeleteBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        @Override
        public DeleteBuilder where(Where where) {
            this.where = where;
            return this;
        }

        @Override
        public DeleteBuilder whereDocument(WhereDocument whereDocument) {
            this.whereDocument = whereDocument;
            return this;
        }

        @Override
        public void execute() {
            boolean hasIds = ids != null && !ids.isEmpty();
            if (!hasIds && where == null && whereDocument == null) {
                throw new IllegalArgumentException(
                        "delete requires at least one criterion: ids, where, or whereDocument"
                );
            }
            String path = ChromaApiPaths.collectionDelete(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.DeleteRequest(
                    ids,
                    where != null ? where.toMap() : null,
                    whereDocument != null ? whereDocument.toMap() : null
            ));
        }
    }

    private static String requireNonBlankField(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ChromaDeserializationException(
                    "Server returned collection with missing " + fieldName,
                    200
            );
        }
        return value.trim();
    }

    private static String requireNonBlankArgument(String fieldName, String value) {
        if (value == null) {
            throw new NullPointerException(fieldName);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        return metadata == null ? null : new LinkedHashMap<String, Object>(metadata);
    }

    private static void validateSizeMatchesIds(String fieldName, List<?> values, int idsSize) {
        if (values != null && values.size() != idsSize) {
            throw new IllegalArgumentException(
                    fieldName + " size must match ids size (" + idsSize + ")"
            );
        }
    }
}
