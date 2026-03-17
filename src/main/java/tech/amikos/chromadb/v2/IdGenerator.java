package tech.amikos.chromadb.v2;

import java.util.Map;

/**
 * Generates record IDs for ChromaDB add and upsert operations.
 *
 * <p>Implementations must be thread-safe. Built-in generators are available as singletons:
 * {@link UuidIdGenerator#INSTANCE}, {@link UlidIdGenerator#INSTANCE},
 * and {@link Sha256IdGenerator#INSTANCE}.</p>
 *
 * <p>As a {@link FunctionalInterface}, custom generators can be provided as lambdas:</p>
 * <pre>{@code
 * collection.add()
 *     .idGenerator((doc, meta) -> "prefix-" + UUID.randomUUID())
 *     .documents("doc1", "doc2")
 *     .embeddings(emb1, emb2)
 *     .execute();
 * }</pre>
 */
@FunctionalInterface
public interface IdGenerator {

    /**
     * Generates an ID for a single record.
     *
     * @param document the document text, may be null for embeddings-only records
     * @param metadata the record metadata, may be null
     * @return a non-null, non-empty ID string
     */
    String generate(String document, Map<String, Object> metadata);
}
