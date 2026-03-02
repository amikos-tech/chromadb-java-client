package tech.amikos.chromadb.v2;

import java.util.List;

/**
 * ChromaDB client interface.
 *
 * <p>All implementations manage mutable tenant/database session context.
 * Use {@link ChromaClient#builder()} for self-hosted deployments or
 * {@link ChromaClient#cloud()} for Chroma Cloud.</p>
 *
 * <p>Session context reads/writes are atomic. Each operation snapshots the active tenant/database
 * at method start and uses that snapshot for the full operation. Multi-call sequences
 * (for example, switching context and then issuing another call) are not externally synchronized
 * across threads.</p>
 *
 * <p>All methods may throw unchecked exceptions from the {@link ChromaException} hierarchy:
 * <ul>
 *   <li>{@link ChromaConnectionException} — network or timeout errors</li>
 *   <li>{@link ChromaDeserializationException} — malformed successful responses</li>
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
     * @throws ChromaUnauthorizedException if authentication is missing/invalid
     * @throws ChromaForbiddenException if access is denied
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

    // --- Session context ---

    /**
     * Switches the active tenant for subsequent tenant-scoped operations.
     *
     * <p>When switching tenant, the active database is reset to
     * {@link Database#defaultDatabase()}.</p>
     *
     * <p>Existing {@link Collection} instances are permanently bound to the tenant/database
     * captured when they were created.</p>
     *
     * <p>This method updates only local client session context and does not verify that the
     * tenant exists on the server.</p>
     *
     * @param tenant tenant to use for subsequent tenant-scoped calls
     * @throws NullPointerException if {@code tenant} is null
     */
    void useTenant(Tenant tenant);

    /**
     * Returns the active tenant in this client session.
     *
     * @return current non-null tenant snapshot
     */
    Tenant currentTenant();

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

    /**
     * Switches the active database for subsequent collection operations.
     *
     * <p>Existing {@link Collection} instances are permanently bound to the tenant/database
     * captured when they were created.</p>
     *
     * <p>This method updates only local client session context and does not verify that the
     * database exists on the server.</p>
     *
     * @param database database to use for subsequent collection calls
     * @throws NullPointerException if {@code database} is null
     */
    void useDatabase(Database database);

    /**
     * Returns the active database in this client session.
     *
     * @return current non-null database snapshot
     */
    Database currentDatabase();

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
     * Retrieves a collection and binds an explicit runtime embedding function for local text-query flows.
     *
     * <p>The embedding function is not sent to the server; it is used client-side for
     * operations such as {@code collection.query().queryTexts(...)}.</p>
     *
     * @throws ChromaNotFoundException if the collection does not exist
     */
    default Collection getCollection(String name, tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction) {
        return getCollection(name);
    }

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
