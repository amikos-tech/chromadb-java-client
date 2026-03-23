package tech.amikos.chromadb.v2;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Package-private {@link Collection} implementation backed by HTTP transport.
 *
 * <p>Mutable properties use {@code volatile} fields so getter calls observe updates across
 * threads. This class does not provide full transactional thread-safety for concurrent
 * mutations; concurrent writes are last-write-wins. Metadata merge updates are synchronized
 * locally to avoid lost updates from concurrent read-modify-write sequences.</p>
 */
final class ChromaHttpCollection implements Collection {

    private static final Logger LOG = Logger.getLogger(ChromaHttpCollection.class.getName());

    private final ChromaApiClient apiClient;
    private final String id;
    private final Tenant tenant;
    private final Database database;

    private volatile String name;
    private volatile Map<String, Object> metadata;
    private volatile Integer dimension;
    private volatile CollectionConfiguration configuration;
    private volatile Schema schema;
    private final tech.amikos.chromadb.embeddings.EmbeddingFunction explicitEmbeddingFunction;
    private volatile tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction;
    private volatile EmbeddingFunctionSpec embeddingFunctionSpec;
    private volatile boolean overrideWarningLogged = false;

    private ChromaHttpCollection(ChromaApiClient apiClient, String id, String name,
                                 Tenant tenant, Database database,
                                 Map<String, Object> metadata, Integer dimension,
                                 CollectionConfiguration configuration,
                                 Schema schema,
                                 tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction,
                                 EmbeddingFunctionSpec embeddingFunctionSpec) {
        this.apiClient = apiClient;
        this.id = id;
        this.name = name;
        this.tenant = tenant;
        this.database = database;
        this.metadata = copyMetadata(metadata);
        this.dimension = dimension;
        this.configuration = configuration;
        this.schema = schema;
        this.explicitEmbeddingFunction = embeddingFunction;
        this.embeddingFunction = embeddingFunction;
        this.embeddingFunctionSpec = embeddingFunctionSpec;
    }

    static ChromaHttpCollection from(ChromaDtos.CollectionResponse dto,
                                     ChromaApiClient apiClient,
                                     Tenant tenant, Database database,
                                     tech.amikos.chromadb.embeddings.EmbeddingFunction explicitEmbeddingFunction) {
        if (dto == null) {
            throw new ChromaDeserializationException(
                    "Server returned an empty collection payload",
                    200
            );
        }
        Objects.requireNonNull(apiClient, "apiClient");
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(database, "database");
        CollectionConfiguration parsedConfiguration = ChromaDtos.parseConfiguration(dto.configurationJson);
        Schema parsedTopLevelSchema = parseTopLevelSchema(dto.schema);
        Schema parsedConfigurationSchema = parsedConfiguration != null ? parsedConfiguration.getSchema() : null;
        Schema effectiveSchema = parsedTopLevelSchema != null ? parsedTopLevelSchema : parsedConfigurationSchema;

        EmbeddingFunctionSpec configSpec = parsedConfiguration != null
                ? parsedConfiguration.getEmbeddingFunction()
                : null;
        EmbeddingFunctionSpec topLevelSchemaSpec = parsedTopLevelSchema != null
                ? parsedTopLevelSchema.getDefaultEmbeddingFunctionSpec()
                : null;
        EmbeddingFunctionSpec configSchemaSpec = parsedConfigurationSchema != null
                ? parsedConfigurationSchema.getDefaultEmbeddingFunctionSpec()
                : null;
        EmbeddingFunctionSpec effectiveSpec = configSpec != null
                ? configSpec
                : (topLevelSchemaSpec != null ? topLevelSchemaSpec : configSchemaSpec);

        return new ChromaHttpCollection(
                apiClient,
                requireNonBlankField("collection.id", dto.id),
                requireNonBlankField("collection.name", dto.name),
                tenant,
                database,
                dto.metadata,
                dto.dimension,
                parsedConfiguration,
                effectiveSchema,
                explicitEmbeddingFunction,
                effectiveSpec
        );
    }

    private static Schema parseTopLevelSchema(Map<String, Object> schemaJson) {
        if (schemaJson == null || schemaJson.isEmpty()) {
            return null;
        }
        try {
            return ChromaDtos.parseSchema(schemaJson);
        } catch (ChromaDeserializationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ChromaDeserializationException(
                    "Server returned invalid collection schema: " + e.getMessage(),
                    200,
                    e
            );
        }
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
    public Schema getSchema() {
        return schema;
    }

    @Override
    public int count() {
        String path = ChromaApiPaths.collectionCount(tenant.getName(), database.getName(), id);
        return apiClient.get(path, Integer.class);
    }

    @Override
    public Collection fork(String newName) {
        String normalizedName = requireNonBlankArgument("newName", newName);
        String path = ChromaApiPaths.collectionFork(tenant.getName(), database.getName(), id);
        ChromaDtos.CollectionResponse resp = apiClient.post(
                path,
                new ChromaDtos.ForkCollectionRequest(normalizedName),
                ChromaDtos.CollectionResponse.class
        );
        return ChromaHttpCollection.from(resp, apiClient, tenant, database, explicitEmbeddingFunction);
    }

    @Override
    public int forkCount() {
        String path = ChromaApiPaths.collectionForkCount(tenant.getName(), database.getName(), id);
        ChromaDtos.ForkCountResponse resp = apiClient.get(path, ChromaDtos.ForkCountResponse.class);
        if (resp.count == null) {
            throw new ChromaDeserializationException(
                    "Server returned fork_count response with missing 'count' field", 200);
        }
        return resp.count;
    }

    @Override
    public IndexingStatus indexingStatus() {
        String path = ChromaApiPaths.collectionIndexingStatus(tenant.getName(), database.getName(), id);
        ChromaDtos.IndexingStatusResponse resp = apiClient.get(path, ChromaDtos.IndexingStatusResponse.class);
        List<String> missing = new ArrayList<String>();
        if (resp.numIndexedOps == null) missing.add("num_indexed_ops");
        if (resp.numUnindexedOps == null) missing.add("num_unindexed_ops");
        if (resp.totalOps == null) missing.add("total_ops");
        if (resp.opIndexingProgress == null) missing.add("op_indexing_progress");
        if (!missing.isEmpty()) {
            throw new ChromaDeserializationException(
                    "Server returned indexing_status response with missing required fields: " + missing, 200);
        }
        return IndexingStatus.of(resp.numIndexedOps, resp.numUnindexedOps, resp.totalOps, resp.opIndexingProgress);
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
        validateConfigurationGroupCompatibility(config);
        Map<String, Object> updatePayload = ChromaDtos.toUpdateConfigurationMap(config);
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
            if (this.configuration.getSchema() != null) {
                builder.schema(this.configuration.getSchema());
            }
            if (this.configuration.getEmbeddingFunction() != null) {
                builder.embeddingFunction(this.configuration.getEmbeddingFunction());
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
        CollectionConfiguration mergedConfiguration;
        try {
            mergedConfiguration = builder.build();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(
                    "cannot mix HNSW and SPANN parameters in local collection configuration",
                    e
            );
        }
        this.configuration = mergedConfiguration;
        if (this.schema == null && mergedConfiguration.getSchema() != null) {
            this.schema = mergedConfiguration.getSchema();
        }
        EmbeddingFunctionSpec previousSpec = this.embeddingFunctionSpec;
        EmbeddingFunctionSpec configSpec = mergedConfiguration.getEmbeddingFunction();
        EmbeddingFunctionSpec topLevelSchemaSpec = this.schema != null
                ? this.schema.getDefaultEmbeddingFunctionSpec()
                : null;
        EmbeddingFunctionSpec configSchemaSpec = mergedConfiguration.getSchema() != null
                ? mergedConfiguration.getSchema().getDefaultEmbeddingFunctionSpec()
                : null;
        EmbeddingFunctionSpec effectiveSpec = configSpec != null
                ? configSpec
                : (topLevelSchemaSpec != null ? topLevelSchemaSpec : configSchemaSpec);
        if (!Objects.equals(previousSpec, effectiveSpec)) {
            this.embeddingFunctionSpec = effectiveSpec;
            if (explicitEmbeddingFunction == null) {
                this.embeddingFunction = null;
            }
        }
    }

    private void validateConfigurationGroupCompatibility(UpdateCollectionConfiguration update) {
        IndexGroup currentGroup = resolveCurrentIndexGroup(this.configuration, this.schema);
        if (currentGroup == IndexGroup.UNKNOWN) {
            return;
        }

        if ((update.hasHnswUpdates() && currentGroup == IndexGroup.SPANN)
                || (update.hasSpannUpdates() && currentGroup == IndexGroup.HNSW)) {
            throw new IllegalArgumentException(
                    "cannot switch collection index parameters between HNSW and SPANN in modifyConfiguration"
            );
        }
    }

    private static IndexGroup resolveCurrentIndexGroup(CollectionConfiguration configuration, Schema schema) {
        if (configuration != null) {
            boolean hasFlatHnsw = hasAnyHnswParameters(configuration);
            boolean hasFlatSpann = hasAnySpannParameters(configuration);
            if (hasFlatHnsw && !hasFlatSpann) {
                return IndexGroup.HNSW;
            }
            if (hasFlatSpann && !hasFlatHnsw) {
                return IndexGroup.SPANN;
            }
        }

        IndexGroup schemaGroup = resolveSchemaIndexGroup(schema);
        if (schemaGroup != IndexGroup.UNKNOWN) {
            return schemaGroup;
        }
        return configuration != null
                ? resolveSchemaIndexGroup(configuration.getSchema())
                : IndexGroup.UNKNOWN;
    }

    private static IndexGroup resolveSchemaIndexGroup(Schema schema) {
        if (schema == null) {
            return IndexGroup.UNKNOWN;
        }
        ValueTypes embeddingValueTypes = schema.getKey(Schema.EMBEDDING_KEY);
        if (embeddingValueTypes == null || embeddingValueTypes.getFloatList() == null) {
            return IndexGroup.UNKNOWN;
        }
        VectorIndexType vectorIndexType = embeddingValueTypes.getFloatList().getVectorIndex();
        if (vectorIndexType == null || vectorIndexType.getConfig() == null) {
            return IndexGroup.UNKNOWN;
        }
        VectorIndexConfig vectorIndexConfig = vectorIndexType.getConfig();
        boolean hasHnsw = vectorIndexConfig.getHnsw() != null;
        boolean hasSpann = vectorIndexConfig.getSpann() != null;
        if (hasHnsw && !hasSpann) {
            return IndexGroup.HNSW;
        }
        if (hasSpann && !hasHnsw) {
            return IndexGroup.SPANN;
        }
        return IndexGroup.UNKNOWN;
    }

    private static boolean hasAnyHnswParameters(CollectionConfiguration configuration) {
        return configuration.getHnswM() != null
                || configuration.getHnswConstructionEf() != null
                || configuration.getHnswSearchEf() != null
                || configuration.getHnswNumThreads() != null
                || configuration.getHnswBatchSize() != null
                || configuration.getHnswSyncThreshold() != null
                || configuration.getHnswResizeFactor() != null;
    }

    private static boolean hasAnySpannParameters(CollectionConfiguration configuration) {
        return configuration.getSpannSearchNprobe() != null
                || configuration.getSpannEfSearch() != null;
    }

    private enum IndexGroup {
        HNSW,
        SPANN,
        UNKNOWN
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

    @Override
    public SearchBuilder search() {
        return new SearchBuilderImpl();
    }

    // --- Builder implementations ---

    private final class AddBuilderImpl implements AddBuilder {
        private List<String> ids;
        private IdGenerator idGenerator;
        private List<float[]> embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<String> uris;

        @Override
        public AddBuilder ids(String... ids) {
            Objects.requireNonNull(ids, "ids");
            if (ids.length > 0) {
                this.ids = Arrays.asList(ids);
            }
            return this;
        }

        @Override
        public AddBuilder ids(List<String> ids) {
            this.ids = Objects.requireNonNull(ids, "ids");
            return this;
        }

        @Override
        public AddBuilder idGenerator(IdGenerator idGenerator) {
            this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
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
            validateMetadataArrayTypes(metadatas);
            List<String> resolvedIds = resolveIds(ids, idGenerator, documents, embeddings, metadatas, uris);
            if (hasExplicitIds(ids)) {
                checkForDuplicateIds(resolvedIds);
            }
            int idsSize = resolvedIds.size();
            String countLabel = hasExplicitIds(ids) ? "ids size" : "record count";
            validateSizeMatchesCount("embeddings", embeddings, idsSize, countLabel);
            validateSizeMatchesCount("documents", documents, idsSize, countLabel);
            validateSizeMatchesCount("metadatas", metadatas, idsSize, countLabel);
            validateSizeMatchesCount("uris", uris, idsSize, countLabel);
            String path = ChromaApiPaths.collectionAdd(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.AddRequest(
                    resolvedIds,
                    ChromaDtos.toFloatLists(embeddings),
                    documents,
                    metadatas,
                    uris
            ));
        }
    }

    private final class UpsertBuilderImpl implements UpsertBuilder {
        private List<String> ids;
        private IdGenerator idGenerator;
        private List<float[]> embeddings;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<String> uris;

        @Override
        public UpsertBuilder ids(String... ids) {
            Objects.requireNonNull(ids, "ids");
            if (ids.length > 0) {
                this.ids = Arrays.asList(ids);
            }
            return this;
        }

        @Override
        public UpsertBuilder ids(List<String> ids) {
            this.ids = Objects.requireNonNull(ids, "ids");
            return this;
        }

        @Override
        public UpsertBuilder idGenerator(IdGenerator idGenerator) {
            this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
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
            validateMetadataArrayTypes(metadatas);
            List<String> resolvedIds = resolveIds(ids, idGenerator, documents, embeddings, metadatas, uris);
            if (hasExplicitIds(ids)) {
                checkForDuplicateIds(resolvedIds);
            }
            int idsSize = resolvedIds.size();
            String countLabel = hasExplicitIds(ids) ? "ids size" : "record count";
            validateSizeMatchesCount("embeddings", embeddings, idsSize, countLabel);
            validateSizeMatchesCount("documents", documents, idsSize, countLabel);
            validateSizeMatchesCount("metadatas", metadatas, idsSize, countLabel);
            validateSizeMatchesCount("uris", uris, idsSize, countLabel);
            String path = ChromaApiPaths.collectionUpsert(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.UpsertRequest(
                    resolvedIds,
                    ChromaDtos.toFloatLists(embeddings),
                    documents,
                    metadatas,
                    uris
            ));
        }
    }

    private final class QueryBuilderImpl implements QueryBuilder {
        private List<float[]> queryEmbeddings;
        private List<String> queryTexts;
        private int nResults = 10;
        private Where where;
        private WhereDocument whereDocument;
        private List<Include> include;

        @Override
        public QueryBuilder queryTexts(String... texts) {
            if (texts == null) {
                throw new NullPointerException("texts");
            }
            return queryTexts(Arrays.asList(texts));
        }

        @Override
        public QueryBuilder queryTexts(List<String> texts) {
            this.queryTexts = validateQueryTexts(texts);
            return this;
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
            List<float[]> resolvedEmbeddings = queryEmbeddings;
            if ((resolvedEmbeddings == null || resolvedEmbeddings.isEmpty())
                    && queryTexts != null && !queryTexts.isEmpty()) {
                resolvedEmbeddings = embedQueryTexts(queryTexts);
            } else if (resolvedEmbeddings != null && !resolvedEmbeddings.isEmpty()
                    && queryTexts != null && !queryTexts.isEmpty()) {
                LOG.fine("queryTexts ignored because queryEmbeddings were also set; "
                        + "explicit embeddings take precedence");
            }
            if (resolvedEmbeddings == null || resolvedEmbeddings.isEmpty()) {
                throw new IllegalArgumentException("queryEmbeddings must be provided");
            }
            Map<String, Object> whereMap = requireNonNullMap(where, "where");
            Map<String, Object> whereDocumentMap = requireNonNullMap(whereDocument, "whereDocument");
            List<String> includeValues = null;
            if (include != null) {
                includeValues = new ArrayList<String>(include.size());
                for (Include inc : include) {
                    includeValues.add(inc.getValue());
                }
            }
            String path = ChromaApiPaths.collectionQuery(tenant.getName(), database.getName(), id);
            ChromaDtos.QueryResponse dto = apiClient.post(path, new ChromaDtos.QueryRequest(
                    ChromaDtos.toFloatLists(resolvedEmbeddings),
                    nResults,
                    whereMap,
                    whereDocumentMap,
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
            Map<String, Object> whereMap = requireNonNullMap(where, "where");
            Map<String, Object> whereDocumentMap = requireNonNullMap(whereDocument, "whereDocument");
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
                    whereMap,
                    whereDocumentMap,
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
            validateMetadataArrayTypes(metadatas);
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
            Map<String, Object> whereMap = requireNonNullMap(where, "where");
            Map<String, Object> whereDocumentMap = requireNonNullMap(whereDocument, "whereDocument");
            String path = ChromaApiPaths.collectionDelete(tenant.getName(), database.getName(), id);
            apiClient.post(path, new ChromaDtos.DeleteRequest(
                    ids,
                    whereMap,
                    whereDocumentMap
            ));
        }
    }

    private final class SearchBuilderImpl implements SearchBuilder {

        private List<Search> searches;
        private Where globalFilter;
        private Integer globalLimit;
        private Integer globalOffset;
        private ReadLevel readLevel;

        @Override
        public SearchBuilder queryText(String text) {
            Objects.requireNonNull(text, "text");
            this.searches = Collections.singletonList(
                    Search.builder().knn(Knn.queryText(text)).build()
            );
            return this;
        }

        @Override
        public SearchBuilder queryEmbedding(float[] embedding) {
            Objects.requireNonNull(embedding, "embedding");
            this.searches = Collections.singletonList(
                    Search.builder().knn(Knn.queryEmbedding(embedding)).build()
            );
            return this;
        }

        @Override
        public SearchBuilder searches(Search... searches) {
            Objects.requireNonNull(searches, "searches");
            for (int i = 0; i < searches.length; i++) {
                if (searches[i] == null) {
                    throw new IllegalArgumentException("searches[" + i + "] must not be null");
                }
            }
            this.searches = Arrays.asList(searches);
            return this;
        }

        @Override
        public SearchBuilder where(Where globalFilter) {
            Objects.requireNonNull(globalFilter, "globalFilter");
            this.globalFilter = globalFilter;
            return this;
        }

        @Override
        public SearchBuilder limit(int limit) {
            if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
            this.globalLimit = limit;
            return this;
        }

        @Override
        public SearchBuilder offset(int offset) {
            if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
            this.globalOffset = offset;
            return this;
        }

        @Override
        public SearchBuilder readLevel(ReadLevel readLevel) {
            Objects.requireNonNull(readLevel, "readLevel");
            this.readLevel = readLevel;
            return this;
        }

        @Override
        public SearchResult execute() {
            if (searches == null || searches.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one search must be specified via queryText(), queryEmbedding(), or searches()");
            }

            // Build effective search list, applying global limit/offset where search has none
            List<Search> effectiveSearches = new ArrayList<Search>(searches.size());
            boolean hasGroupBy = false;
            for (Search s : searches) {
                if (s.getGroupBy() != null) hasGroupBy = true;
                boolean needsLimit = s.getLimit() == null && globalLimit != null;
                boolean needsOffset = s.getOffset() == null && globalOffset != null;
                if (needsLimit || needsOffset) {
                    Search.Builder b = s.toBuilder();
                    if (needsLimit) b.limit(globalLimit);
                    if (needsOffset) b.offset(globalOffset);
                    effectiveSearches.add(b.build());
                } else {
                    effectiveSearches.add(s);
                }
            }

            List<Map<String, Object>> searchItems = new ArrayList<Map<String, Object>>(effectiveSearches.size());
            for (Search s : effectiveSearches) {
                searchItems.add(ChromaDtos.buildSearchItemMap(s, globalFilter));
            }
            String rl = readLevel != null ? readLevel.getValue() : null;
            ChromaDtos.SearchRequest request = new ChromaDtos.SearchRequest(searchItems, rl);

            String path = ChromaApiPaths.collectionSearch(tenant.getName(), database.getName(), id);
            ChromaDtos.SearchResponse dto = apiClient.post(path, request, ChromaDtos.SearchResponse.class);
            return SearchResultImpl.from(dto, hasGroupBy);
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
        validateSizeMatchesCount(fieldName, values, idsSize, "ids size");
    }

    private static void validateSizeMatchesCount(String fieldName, List<?> values, int expectedSize, String countLabel) {
        if (values != null && values.size() != expectedSize) {
            throw new IllegalArgumentException(
                    fieldName + " size must match " + countLabel + " (" + expectedSize + ")"
            );
        }
    }

    private static boolean hasExplicitIds(List<String> ids) {
        return ids != null && !ids.isEmpty();
    }

    private static boolean hasIdsArgument(List<String> ids) {
        return ids != null;
    }

    private static List<String> resolveIds(List<String> ids, IdGenerator idGenerator,
                                            List<String> documents, List<float[]> embeddings,
                                            List<Map<String, Object>> metadatas, List<String> uris) {
        boolean hasIdsArg = hasIdsArgument(ids);
        boolean hasIds = hasExplicitIds(ids);
        boolean hasGenerator = idGenerator != null;
        if (hasIdsArg && hasGenerator) {
            throw new IllegalArgumentException("cannot set both ids and idGenerator");
        }
        if (!hasIds && !hasGenerator) {
            throw new IllegalArgumentException("ids must not be empty");
        }
        if (hasIds) {
            return ids;
        }
        int count = inferRecordCount(documents, embeddings, metadatas, uris);
        return generateIds(idGenerator, count, documents, metadatas);
    }

    private static int inferRecordCount(List<String> documents, List<float[]> embeddings,
                                         List<Map<String, Object>> metadatas, List<String> uris) {
        String[] names = {"documents", "embeddings", "metadatas", "uris"};
        List<?>[] fields = {documents, embeddings, metadatas, uris};

        Integer count = null;
        boolean mismatch = false;
        boolean hasPendingZeroSizedField = false;
        List<String> sizeDetails = new ArrayList<String>(4);

        for (int f = 0; f < fields.length; f++) {
            if (fields[f] == null) {
                continue;
            }
            int size = fields[f].size();
            sizeDetails.add(names[f] + "=" + size);
            if (count == null) {
                if (size > 0) {
                    count = Integer.valueOf(size);
                    if (hasPendingZeroSizedField) {
                        mismatch = true;
                    }
                } else {
                    hasPendingZeroSizedField = true;
                }
            } else if (size != count.intValue()) {
                mismatch = true;
            }
        }

        if (count == null) {
            if (hasPendingZeroSizedField) {
                throw new IllegalArgumentException(
                        "all provided data fields are empty; idGenerator cannot infer record count: "
                                + String.join(", ", sizeDetails)
                );
            }
            throw new IllegalArgumentException(
                    "idGenerator requires at least one data field (documents, embeddings, metadatas, or uris) to infer record count"
            );
        }
        if (mismatch) {
            throw new IllegalArgumentException(
                    "all data fields must have the same size when idGenerator is used: "
                            + String.join(", ", sizeDetails)
            );
        }
        return count.intValue();
    }

    /**
     * Generates IDs for records using the provided generator, with client-side validation.
     *
     * <p>Throws {@link ChromaException} (not {@code IllegalArgumentException}) for all
     * generator failures: null/blank output, runtime exceptions, and duplicate IDs.
     * This is intentional for the v2 API — all client-side validation errors use the
     * {@code ChromaException} hierarchy for consistency.</p>
     */
    private static List<String> generateIds(IdGenerator generator, int count,
                                             List<String> documents,
                                             List<Map<String, Object>> metadatas) {
        List<String> ids = new ArrayList<String>(count);
        Map<String, List<Integer>> indexesById = new LinkedHashMap<String, List<Integer>>();
        boolean hasDuplicate = false;
        for (int i = 0; i < count; i++) {
            String doc = documents != null ? documents.get(i) : null;
            Map<String, Object> meta = metadatas != null ? metadatas.get(i) : null;
            String generated;
            try {
                generated = generator.generate(doc, meta);
            } catch (RuntimeException e) {
                throw new ChromaException(
                        "IdGenerator threw an exception at record index " + i + ": " + e.toString(),
                        e
                );
            }
            if (generated == null || generated.isEmpty()) {
                throw new ChromaException(
                        "IdGenerator returned null or empty ID at index " + i
                );
            }
            List<Integer> indexes = indexesById.get(generated);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                indexesById.put(generated, indexes);
            } else {
                hasDuplicate = true;
            }
            indexes.add(Integer.valueOf(i));
            ids.add(generated);
        }
        if (hasDuplicate) {
            throw new ChromaException(buildDuplicateIdsMessage(indexesById));
        }
        return ids;
    }

    private static String buildDuplicateIdsMessage(Map<String, List<Integer>> indexesById) {
        List<String> details = new ArrayList<String>(indexesById.size());
        for (Map.Entry<String, List<Integer>> entry : indexesById.entrySet()) {
            if (entry.getValue().size() > 1) {
                details.add("'" + entry.getKey() + "' at indexes " + entry.getValue());
            }
        }
        if (details.isEmpty()) {
            return "IdGenerator produced duplicate IDs in the same batch";
        }
        return "IdGenerator produced duplicate IDs in the same batch: " + String.join(", ", details);
    }

    /**
     * Checks explicit ID lists for duplicates before sending to server.
     *
     * <p>Uses O(n) detection via LinkedHashMap to preserve insertion order for error messages.</p>
     *
     * @throws ChromaException if duplicate IDs are found, listing the duplicate values and their indexes
     */
    private static void checkForDuplicateIds(List<String> ids) {
        if (ids == null || ids.size() < 2) {
            return;
        }
        Map<String, List<Integer>> indexesById = new LinkedHashMap<String, List<Integer>>();
        boolean hasDuplicate = false;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            List<Integer> indexes = indexesById.get(id);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                indexesById.put(id, indexes);
            } else {
                hasDuplicate = true;
            }
            indexes.add(Integer.valueOf(i));
        }
        if (hasDuplicate) {
            List<String> details = new ArrayList<String>();
            for (Map.Entry<String, List<Integer>> entry : indexesById.entrySet()) {
                if (entry.getValue().size() > 1) {
                    details.add("'" + entry.getKey() + "' at indexes " + entry.getValue());
                }
            }
            throw new ChromaException(
                    "Duplicate IDs in add/upsert batch: " + String.join(", ", details)
            );
        }
    }

    private static Map<String, Object> requireNonNullMap(Where where, String fieldName) {
        if (where == null) {
            return null;
        }
        Map<String, Object> map = where.toMap();
        if (map == null) {
            throw new IllegalArgumentException(fieldName + ".toMap() must not return null");
        }
        return map;
    }

    private static Map<String, Object> requireNonNullMap(WhereDocument whereDocument, String fieldName) {
        if (whereDocument == null) {
            return null;
        }
        Map<String, Object> map = whereDocument.toMap();
        if (map == null) {
            throw new IllegalArgumentException(fieldName + ".toMap() must not return null");
        }
        return map;
    }

    /**
     * Resolves the embedding function for text embedding operations.
     *
     * <p><strong>Precedence (highest to lowest):</strong></p>
     * <ol>
     *   <li>Runtime/explicit EF -- set via {@code CreateCollectionOptions.embeddingFunction(...)}
     *       or {@code client.getCollection(name, embeddingFunction)}. Always wins.</li>
     *   <li>{@code configuration.embedding_function} -- persisted in collection configuration.</li>
     *   <li>{@code schema.default_embedding_function} -- persisted in collection schema.</li>
     * </ol>
     *
     * <p>When an explicit EF is provided and a persisted EF descriptor also exists,
     * a WARNING is logged. The explicit EF is used; no error is thrown.</p>
     *
     * <p>Unsupported EF descriptors (unknown provider name) do not block collection
     * construction. They fail lazily at the first embed operation.</p>
     */
    private synchronized tech.amikos.chromadb.embeddings.EmbeddingFunction requireEmbeddingFunction() {
        if (explicitEmbeddingFunction != null) {
            if (embeddingFunctionSpec != null && !overrideWarningLogged) {
                LOG.warning("Runtime embedding function overrides persisted collection EF '"
                    + embeddingFunctionSpec.getName() + "'. Explicit EF takes precedence.");
                overrideWarningLogged = true;
            }
            if (embeddingFunction != explicitEmbeddingFunction) {
                embeddingFunction = explicitEmbeddingFunction;
            }
            return explicitEmbeddingFunction;
        }
        if (embeddingFunction != null) {
            return embeddingFunction;
        }
        if (embeddingFunctionSpec == null) {
            throw new ChromaException(
                    "queryTexts requires an embedding function, but none is available in runtime options or collection configuration. "
                            + "Provide a runtime embedding function when creating/retrieving the collection "
                            + "(CreateCollectionOptions.embeddingFunction(...) or client.getCollection(name, embeddingFunction)), "
                            + "set configuration.embedding_function, or use queryEmbeddings(...)."
            );
        }
        LOG.fine("Auto-wired embedding function: " + embeddingFunctionSpec.getName()
            + " from collection configuration");
        embeddingFunction = EmbeddingFunctionResolver.resolve(embeddingFunctionSpec);
        return embeddingFunction;
    }

    private List<float[]> embedQueryTexts(List<String> texts) {
        tech.amikos.chromadb.embeddings.EmbeddingFunction runtimeEmbeddingFunction = requireEmbeddingFunction();
        List<Embedding> embeddings;
        try {
            embeddings = runtimeEmbeddingFunction.embedQueries(texts);
        } catch (ChromaException e) {
            throw e;
        } catch (EFException e) {
            throw new ChromaException("Failed to embed queryTexts: " + e.toString(), e);
        } catch (RuntimeException e) {
            throw new ChromaException("Failed to embed queryTexts: " + e.toString(), e);
        }
        if (embeddings == null) {
            throw new ChromaException("Failed to embed queryTexts: embedding function returned null");
        }
        if (embeddings.size() != texts.size()) {
            throw new ChromaException(
                    "Failed to embed queryTexts: embedding function returned "
                            + embeddings.size()
                            + " embeddings for "
                            + texts.size()
                            + " query texts"
            );
        }
        List<float[]> vectors = new ArrayList<float[]>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            Embedding embedding = embeddings.get(i);
            if (embedding == null) {
                throw new ChromaException(
                        "Failed to embed queryTexts: embedding function returned null at index " + i
                );
            }
            float[] vector = embedding.asArray();
            if (vector == null) {
                throw new ChromaException(
                        "Failed to embed queryTexts: embedding function returned null vector at index " + i
                );
            }
            vectors.add(vector);
        }
        return vectors;
    }

    /**
     * Validates that all List values in metadata maps contain homogeneous types.
     * Mixed-type arrays (e.g., ["foo", 42, true]) are rejected before sending to server.
     *
     * @throws ChromaBadRequestException if any metadata map contains a List with mixed types or null elements
     */
    static void validateMetadataArrayTypes(List<Map<String, Object>> metadatas) {
        if (metadatas == null) {
            return;
        }
        for (int i = 0; i < metadatas.size(); i++) {
            Map<String, Object> meta = metadatas.get(i);
            if (meta == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : meta.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    validateHomogeneousList(entry.getKey(), (List<?>) value, i);
                }
            }
        }
    }

    private static void validateHomogeneousList(String key, List<?> list, int recordIndex) {
        if (list.isEmpty()) {
            return; // empty arrays are valid
        }
        Class<?> firstType = null;
        for (int j = 0; j < list.size(); j++) {
            Object element = list.get(j);
            if (element == null) {
                throw new ChromaBadRequestException(
                        "metadata[" + recordIndex + "]." + key + "[" + j + "] is null; "
                                + "array metadata values must not contain null elements",
                        "NULL_ARRAY_ELEMENT"
                );
            }
            Class<?> normalizedType = normalizeNumericType(element.getClass());
            if (firstType == null) {
                firstType = normalizedType;
            } else if (!firstType.equals(normalizedType)) {
                throw new ChromaBadRequestException(
                        "metadata[" + recordIndex + "]." + key + " contains mixed types: "
                                + "expected " + firstType.getSimpleName() + " but found "
                                + element.getClass().getSimpleName() + " at index " + j
                                + "; array metadata values must be homogeneous",
                        "MIXED_TYPE_ARRAY"
                );
            }
        }
    }

    /**
     * Normalizes numeric types to a common base for homogeneity comparison.
     * Integer, Long, Short, Byte -> Integer (integer group)
     * Float, Double -> Float (floating group)
     */
    private static Class<?> normalizeNumericType(Class<?> clazz) {
        if (clazz == Integer.class || clazz == Long.class || clazz == Short.class || clazz == Byte.class) {
            return Integer.class;
        }
        if (clazz == Float.class || clazz == Double.class) {
            return Float.class;
        }
        return clazz;
    }

    private static List<String> validateQueryTexts(List<String> texts) {
        if (texts == null) {
            throw new NullPointerException("texts");
        }
        if (texts.isEmpty()) {
            throw new IllegalArgumentException("queryTexts must not be empty");
        }
        List<String> copy = new ArrayList<String>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null) {
                throw new IllegalArgumentException("queryTexts[" + i + "] must not be null");
            }
            copy.add(text);
        }
        return copy;
    }
}
