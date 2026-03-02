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
    private volatile Schema schema;
    private volatile tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction;
    private volatile EmbeddingFunctionSpec embeddingFunctionSpec;

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
        Schema parsedTopLevelSchema = ChromaDtos.parseSchema(dto.schema);
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
        CollectionConfiguration mergedConfiguration = builder.build();
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
            this.embeddingFunction = null;
        }
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
            if (queryEmbeddings != null) {
                throw new IllegalArgumentException(
                        "cannot set both queryTexts and queryEmbeddings in the same query"
                );
            }
            this.queryTexts = validateQueryTexts(texts);
            return this;
        }

        @Override
        public QueryBuilder queryEmbeddings(float[]... embeddings) {
            if (queryTexts != null) {
                throw new IllegalArgumentException(
                        "cannot set both queryTexts and queryEmbeddings in the same query"
                );
            }
            this.queryEmbeddings = Arrays.asList(embeddings);
            return this;
        }

        @Override
        public QueryBuilder queryEmbeddings(List<float[]> embeddings) {
            if (queryTexts != null) {
                throw new IllegalArgumentException(
                        "cannot set both queryTexts and queryEmbeddings in the same query"
                );
            }
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

    private synchronized tech.amikos.chromadb.embeddings.EmbeddingFunction requireEmbeddingFunction() {
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
            throw new ChromaException("Failed to embed queryTexts: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ChromaException("Failed to embed queryTexts: " + e.getMessage(), e);
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
