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

    /**
     * Returns a heartbeat timestamp confirming the server is reachable.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConnectionException if the server is unreachable
     */
    String heartbeat();

    /**
     * Returns the server version string.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConnectionException if the server is unreachable
     */
    String version();

    /**
     * Performs server capability discovery and returns operational limits.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConnectionException if the server is unreachable
     */
    PreFlightInfo preFlight();

    /**
     * Returns identity details for the currently authenticated principal.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConnectionException if the server is unreachable
     * @throws ChromaUnauthorizedException if authentication is missing/invalid
     * @throws ChromaForbiddenException if access is denied
     */
    Identity getIdentity();

    /**
     * Resets the server, removing all tenants, databases, and collections.
     *
     * <p><strong>Availability:</strong> Self-hosted only. Not available on Chroma Cloud.</p>
     *
     * @throws ChromaServerException if the server rejects the reset
     */
    void reset();

    // --- Tenant ---

    /**
     * Creates a new tenant with the given name.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConflictException if the tenant already exists
     * @throws ChromaServerException on server errors
     */
    Tenant createTenant(String name);

    /**
     * Returns the tenant with the given name.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
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
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud (client-side only).</p>
     *
     * @param tenant tenant to use for subsequent tenant-scoped calls
     * @throws NullPointerException if {@code tenant} is null
     */
    void useTenant(Tenant tenant);

    /**
     * Returns the active tenant in this client session.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud (client-side only).</p>
     *
     * @return current non-null tenant snapshot
     */
    Tenant currentTenant();

    // --- Database ---

    /**
     * Creates a new database with the given name in the active tenant.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConflictException if the database already exists
     * @throws ChromaServerException on server errors
     */
    Database createDatabase(String name);

    /**
     * Returns the database with the given name in the active tenant.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
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
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud (client-side only).</p>
     *
     * @param database database to use for subsequent collection calls
     * @throws NullPointerException if {@code database} is null
     */
    void useDatabase(Database database);

    /**
     * Returns the active database in this client session.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud (client-side only).</p>
     *
     * @return current non-null database snapshot
     */
    Database currentDatabase();

    /**
     * Lists all databases in the active tenant.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    List<Database> listDatabases();

    /**
     * Deletes the database with the given name from the active tenant.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaNotFoundException if the database does not exist
     */
    void deleteDatabase(String name);

    // --- Collection lifecycle ---

    /**
     * Creates a new collection with the given name in the active tenant and database.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConflictException if a collection with this name already exists
     * @throws ChromaServerException on server errors
     */
    Collection createCollection(String name);

    /**
     * Creates a new collection with the given name and options in the active tenant and database.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaConflictException if a collection with this name already exists
     * @throws ChromaServerException on server errors
     */
    Collection createCollection(String name, CreateCollectionOptions options);

    /**
     * Returns the collection with the given name.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaNotFoundException if the collection does not exist
     */
    Collection getCollection(String name);

    /**
     * Retrieves a collection and binds an explicit runtime embedding function for local text-query flows.
     *
     * <p>The embedding function is not sent to the server; it is used client-side for
     * operations such as {@code collection.query().queryTexts(...)}.</p>
     *
     * <p>The default implementation keeps backward compatibility by delegating to
     * {@link #getCollection(String)} and does not bind {@code embeddingFunction}.
     * Implementations should override this method to attach runtime embedding functions.</p>
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaNotFoundException if the collection does not exist
     */
    default Collection getCollection(String name, tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction) {
        return getCollection(name);
    }

    /**
     * Returns an existing collection with the given name, or creates it if it does not exist.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    Collection getOrCreateCollection(String name);

    /**
     * Returns an existing collection with the given name and options, or creates it if it does not exist.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    Collection getOrCreateCollection(String name, CreateCollectionOptions options);

    /**
     * Lists all collections in the active tenant and database.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    List<Collection> listCollections();

    /**
     * Lists collections in the active tenant and database with pagination.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    List<Collection> listCollections(int limit, int offset);

    /**
     * Deletes the collection with the given name from the active tenant and database.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaNotFoundException if the collection does not exist
     */
    void deleteCollection(String name);

    /**
     * Returns the number of collections in the active tenant and database.
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud.</p>
     *
     * @throws ChromaServerException on server errors
     */
    int countCollections();

    // --- AutoCloseable ---

    /**
     * Closes the client and releases any underlying resources (e.g., connection pool).
     *
     * <p><strong>Availability:</strong> Self-hosted and Chroma Cloud (client-side only).</p>
     */
    @Override
    void close();
}
