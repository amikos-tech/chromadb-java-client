package tech.amikos.chromadb;

import kotlin.Pair;
import kotlin.Triple;
import tech.amikos.chromadb.handler.ApiException;

import tech.amikos.chromadb.ids.IdGenerator;
import tech.amikos.chromadb.model.AddEmbedding;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionBuilder {

    private String name;
    private MetadataBuilder metadataBuilder;
    private EmbeddingFunction embeddingFunction;

    private Client client;
    private Boolean createOrGet;

    private IdGenerator idGenerator;
    private Map<String, Pair<String, List<Float>>> idsDocumentsEmbeddings = null;

    private CollectionBuilder() {
    }

    public static CollectionBuilder instance(Client client, String name) {
        CollectionBuilder instance = new CollectionBuilder();
        instance.client = client;
        instance.name = name;
        return instance;
    }

    public static CollectionBuilder instance(Client client) {
        CollectionBuilder instance = new CollectionBuilder();
        instance.client = client;
        return instance;
    }

    /**
     * Set the name of the collection
     *
     * @param name collection name
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Flag to create the collection if it does not exist
     *
     * @param createOrGet flag to create the collection if it does not exist
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withCreateOrGet(Boolean createOrGet) {
        this.createOrGet = createOrGet;
        return this;
    }

    /**
     * Adds Id Generator to the collection used for generating Ids for documents and embeddings
     *
     * @param idGenerator {@link IdGenerator}
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        return this;
    }

    /**
     * Add embedding to the collection
     *
     * @param id        id of the embedding
     * @param document  document of the embedding
     * @param embedding embedding to add
     */
    private void addEmbedding(String id, String document, List<Float> embedding) {
        if (this.idsDocumentsEmbeddings == null) {
            this.idsDocumentsEmbeddings = new HashMap<>();
        }
        Pair<String, List<Float>> pair = new Pair<>(document, embedding);
        this.idsDocumentsEmbeddings.put(id, pair);
    }

    /**
     * Add document with its Id to the collection. Existing Id will be overwritten.
     *
     * @param document document to add
     * @param id       id of the document
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withDocument(String document, String id) {
        this.addEmbedding(id, document, null);
        return this;
    }

    /**
     * Add document with its Id and Embedding to the collection. Existing Id will be overwritten.
     *
     * @param document  document to add
     * @param embedding embedding of the document
     * @param id        id of the document
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withDocument(String document, List<Float> embedding, String id) {
        this.addEmbedding(id, document, embedding);
        return this;
    }

    /**
     * Add document to the collection. Id will be generated using the Id Generator.
     *
     * @param document document to add
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withDocument(String document) {
        if (document == null) {
            throw new IllegalArgumentException("Cannot add document without Id Generator. Please specify Id Generator with '.withIdGenerator()' method.");
        }
        String id = this.idGenerator.generateForDocument(document);
        this.addEmbedding(id, document, null);
        return this;
    }

    /**
     * Add embedding to the collection. Id will be generated using the Id Generator.
     *
     * @param embedding embedding to add
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withEmbedding(List<Float> embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("Cannot add embedding without Id Generator. Please specify Id Generator with '.withIdGenerator()' method.");
        }
        String id = this.idGenerator.generateForDocument(embedding.toString());
        this.addEmbedding(id, null, embedding);
        return this;
    }

    /**
     * Add embedding with its Id to the collection. Existing Id will be overwritten.
     *
     * @param embedding embedding to add
     * @param id        id of the embedding
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withEmbedding(List<Float> embedding, String id) {
        this.addEmbedding(id, null, embedding);
        return this;
    }

    /**
     * Add documents to the collection. Ids will be generated using the Id Generator.
     *
     * @param documents documents to add
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withDocuments(List<String> documents) {
        if (idGenerator == null) {
            throw new IllegalArgumentException("Cannot add documents without Id Generator. Please specify Id Generator with '.withIdGenerator()' method.");
        }
        for (String document : documents) {
            this.withDocument(document);
        }
        return this;
    }

    /**
     * Add documents with their Ids to the collection. Existing Ids will be overwritten.
     *
     * @param documents documents to add
     * @param ids       ids of the documents
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withDocuments(List<String> documents, List<String> ids) {
        if (documents == null || ids == null) {
            throw new IllegalArgumentException("Documents and Ids must be provided");
        }
        if (documents.size() != ids.size()) {
            throw new IllegalArgumentException("Documents and Ids must be of the same size");
        }
        for (int i = 0; i < documents.size(); i++) {
            this.withDocument(documents.get(i), ids.get(i));
        }
        return this;
    }

    /**
     * Add embeddings to the collection. Ids will be generated using the Id Generator.
     *
     * @param embeddings embeddings to add
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withEmbeddings(List<List<Float>> embeddings) {
        if (idGenerator == null) {
            throw new IllegalArgumentException("Cannot add embeddings without Id Generator. Please specify Id Generator with '.withIdGenerator()' method.");
        }
        for (List<Float> embedding : embeddings) {
            this.withEmbedding(embedding);
        }
        return this;
    }

    /**
     * Add embeddings with their Ids to the collection. Existing Ids will be overwritten.
     *
     * @param embeddings embeddings to add
     * @param ids        ids of the embeddings
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withEmbeddings(List<List<Float>> embeddings, List<String> ids) {
        if (embeddings == null || ids == null) {
            throw new IllegalArgumentException("Embeddings and Ids must be provided");
        }
        if (embeddings.size() != ids.size()) {
            throw new IllegalArgumentException("Embeddings and Ids must be of the same size");
        }
        for (int i = 0; i < embeddings.size(); i++) {
            this.withEmbedding(embeddings.get(i), ids.get(i));
        }
        return this;
    }

    /**
     * Add metadata key-value pair to the collection
     *
     * @param key   metadata key
     * @param value metadata value
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withMetadata(String key, Object value) {
        if (this.metadataBuilder == null) {
            this.metadataBuilder = MetadataBuilder.create();
        }
        this.metadataBuilder.forValue(key, value);
        return this;
    }

    /**
     * Define the embedding function to use for the collection
     *
     * @param embeddingFunction {@link EmbeddingFunction}
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withEmbeddingFunction(EmbeddingFunction embeddingFunction) {
        this.embeddingFunction = embeddingFunction;
        return this;
    }

    /**
     * The distance function to use for HNSW. The default is L2.
     *
     * @param hnswDistanceFunction {@link HnswDistanceFunction}
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWDistanceFunction(HnswDistanceFunction hnswDistanceFunction) {
        this.withMetadata("hnsw:space", hnswDistanceFunction.getValue());
        return this;
    }

    /***
     * The number of vectors to keep in bruteforce index, before adding to  HNSW. The default is 100.
     * @param batchSize number of vectors to keep in bruteforce index
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWBatchSize(int batchSize) {
        this.withMetadata("hnsw:batch_size", batchSize);
        return this;
    }

    /**
     * The maximum number of vectors to add to HNSW before syncing to disk. The default is 1000.
     *
     * @param syncThreshold number of vectors to add to HNSW before syncing to disk
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWSyncThreshold(int syncThreshold) {
        this.withMetadata("hnsw:sync_threshold", syncThreshold);
        return this;
    }

    /**
     * The maximum number of outgoing connections to neighbors to maintain in HNSW. The default is 16.
     *
     * @param m number of neighbors to consider during construction of HNSW
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWM(int m) {
        this.withMetadata("hnsw:M", m);
        return this;
    }

    /**
     * The number of neighbors to consider during construction of HNSW. The default is 100.
     *
     * @param ef number of neighbors to consider during construction of HNSW
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWConstructionEf(int ef) {
        this.withMetadata("hnsw:construction_ef", ef);
        return this;
    }

    /**
     * The number of neighbors to search for in HNSW. The default is 10.
     *
     * @param ef number of neighbors to search for in HNSW
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWSearchEf(int ef) {
        this.withMetadata("hnsw:search_ef", ef);
        return this;
    }

    /**
     * Number of threads to use for HNSW. The default is number of cores.
     *
     * @param numThreads number of threads to use for HNSW
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWNumThreads(int numThreads) {
        this.withMetadata("hnsw:num_threads", numThreads);
        return this;
    }

    /**
     * The resize factor for HNSW. The default is 1.2.
     *
     * @param resizeFactor resize factor for HNSW
     * @return {@link CollectionBuilder}
     */
    public CollectionBuilder withHNSWResizeFactor(float resizeFactor) {
        this.withMetadata("hnsw:resize_factor", resizeFactor);
        return this;
    }

    private Triple<List<String>, List<String>, List<List<Float>>> validateEmbeddings() {
        int dims = -1;
        if (this.idsDocumentsEmbeddings != null) {
            List<String> documents = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();
            for (Map.Entry<String, Pair<String, List<Float>>> entry : this.idsDocumentsEmbeddings.entrySet()) {
                String id = entry.getKey();
                Pair<String, List<Float>> pair = entry.getValue();
                String document = pair.getFirst();
                List<Float> embedding = pair.getSecond();
                if (dims == -1 && embedding != null) {
                    dims = embedding.size();
                } else if (embedding != null && dims != embedding.size()) {
                    throw new IllegalArgumentException("Embeddings must have the same dimensions");
                }
                if (id == null) {
                    throw new IllegalArgumentException("Id is required");
                }
                if (document == null && embedding == null) {
                    throw new IllegalArgumentException("Document or Embedding is required");
                }
                if (document != null && embedding == null) {
                    embedding = this.embeddingFunction.createEmbedding(Collections.singletonList(document)).get(0);
                }

                documents.add(document);
                ids.add(id);
                embeddings.add(embedding);
            }
            return new Triple<>(ids, documents, embeddings);
        }
        return null;
    }

    public Collection create() throws ApiException {
        if (this.name == null) {
            throw new IllegalArgumentException("Collection name is required");
        }
        if (this.embeddingFunction == null && this.idsDocumentsEmbeddings != null && !this.idsDocumentsEmbeddings.values().stream().allMatch(pair -> pair.getSecond() == null)) {
            throw new IllegalArgumentException("Embedding function is required to add documents where embeddings are not provided.");
        }
        Collection collection = this.client.createCollection(this.name, this.metadataBuilder.build(), this.createOrGet, this.embeddingFunction);

        Triple<List<String>, List<String>, List<List<Float>>> triple = this.validateEmbeddings();
        if (triple != null) {
            collection.add(new AddEmbedding().ids(triple.getFirst()).documents(triple.getSecond()).embeddings(Arrays.asList(triple.getThird().toArray())));
        }
        return collection;

    }
}
