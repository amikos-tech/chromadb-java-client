package tech.amikos.chromadb.v2;

import java.util.List;

/**
 * ChromaDB client interface.
 *
 * <p>All implementations manage tenant/database context set at construction time.
 * Use {@link ChromaClient#builder()} for self-hosted deployments or
 * {@link ChromaClient#cloud()} for Chroma Cloud.</p>
 *
 * <p>All methods may throw unchecked exceptions from the {@link ChromaException} hierarchy:
 * <ul>
 *   <li>{@link ChromaConnectionException} — network or timeout errors</li>
 *   <li>{@link ChromaClientException} — HTTP 4xx errors</li>
 *   <li>{@link ChromaServerException} — HTTP 5xx errors</li>
 * </ul>
 */
public interface Client extends AutoCloseable {

    // --- Health & info ---

    /** @throws ChromaConnectionException if the server is unreachable */
    String heartbeat();

    /** @throws ChromaConnectionException if the server is unreachable */
    String version();

    /**
     * Performs server capability discovery and returns operational limits.
     *
     * @throws ChromaConnectionException if the server is unreachable
     */
    PreFlightInfo preFlight();

    /**
     * Returns identity details for the currently authenticated principal.
     *
     * @throws ChromaConnectionException if the server is unreachable
     */
    Identity getIdentity();

    /** @throws ChromaServerException if the server rejects the reset */
    void reset();

    // --- Tenant ---

    /**
     * @throws ChromaConflictException if the tenant already exists
     * @throws ChromaServerException on server errors
     */
    Tenant createTenant(String name);

    /**
     * @throws ChromaNotFoundException if the tenant does not exist
     */
    Tenant getTenant(String name);

    // --- Database ---

    /**
     * @throws ChromaConflictException if the database already exists
     * @throws ChromaServerException on server errors
     */
    Database createDatabase(String name);

    /**
     * @throws ChromaNotFoundException if the database does not exist
     */
    Database getDatabase(String name);

    /** @throws ChromaServerException on server errors */
    List<Database> listDatabases();

    /**
     * @throws ChromaNotFoundException if the database does not exist
     */
    void deleteDatabase(String name);

    // --- Collection lifecycle ---

    /**
     * @throws ChromaConflictException if a collection with this name already exists
     * @throws ChromaServerException on server errors
     */
    Collection createCollection(String name);

    /**
     * @throws ChromaConflictException if a collection with this name already exists
     * @throws ChromaServerException on server errors
     */
    Collection createCollection(String name, CreateCollectionOptions options);

    /**
     * @throws ChromaNotFoundException if the collection does not exist
     */
    Collection getCollection(String name);

    /**
     * @throws ChromaServerException on server errors
     */
    Collection getOrCreateCollection(String name);

    /**
     * @throws ChromaServerException on server errors
     */
    Collection getOrCreateCollection(String name, CreateCollectionOptions options);

    /** @throws ChromaServerException on server errors */
    List<Collection> listCollections();

    /** @throws ChromaServerException on server errors */
    List<Collection> listCollections(int limit, int offset);

    /**
     * @throws ChromaNotFoundException if the collection does not exist
     */
    void deleteCollection(String name);

    /** @throws ChromaServerException on server errors */
    int countCollections();

    // --- AutoCloseable ---

    @Override
    void close();
}
