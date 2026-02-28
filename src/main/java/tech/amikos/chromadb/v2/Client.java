package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/**
 * ChromaDB client interface.
 *
 * <p>All implementations manage tenant/database context set at construction time.
 * Use {@link ChromaClient#builder()} to create instances.</p>
 */
public interface Client extends AutoCloseable {

    // --- Health & info ---

    String heartbeat();

    String version();

    void reset();

    // --- Tenant ---

    Tenant createTenant(String name);

    Tenant getTenant(String name);

    // --- Database ---

    Database createDatabase(String name);

    Database getDatabase(String name);

    List<Database> listDatabases();

    void deleteDatabase(String name);

    // --- Collection lifecycle ---

    Collection createCollection(String name);

    Collection createCollection(String name, CreateCollectionOptions options);

    Collection getCollection(String name);

    Collection getOrCreateCollection(String name);

    Collection getOrCreateCollection(String name, CreateCollectionOptions options);

    List<Collection> listCollections();

    List<Collection> listCollections(int limit, int offset);

    void deleteCollection(String name);

    int countCollections();

    // --- AutoCloseable ---

    @Override
    void close();
}
