package tech.amikos.chromadb.v2.client;

import tech.amikos.chromadb.v2.model.Database;
import tech.amikos.chromadb.v2.model.Tenant;
import tech.amikos.chromadb.v2.model.CreateCollectionRequest;
import tech.amikos.chromadb.v2.model.UpdateTenantRequest;
import tech.amikos.chromadb.v2.model.UpdateCollectionRequest;

import java.util.List;
import java.util.function.Consumer;

public interface Client {

    // Tenant operations
    Tenant createTenant(String name);
    Tenant getTenant(String name);
    void updateTenant(String name, Consumer<UpdateTenantRequest.Builder> configurator);

    // Database operations
    Database createDatabase(String tenant, String name);
    Database getDatabase(String tenant, String name);
    List<Database> listDatabases(String tenant);
    List<Database> listDatabases(String tenant, Integer limit, Integer offset);
    void deleteDatabase(String tenant, String name);

    // Collection operations
    Collection createCollection(String tenant, String database, String name);
    Collection createCollection(String tenant, String database, String name,
                                Consumer<CreateCollectionRequest.Builder> configurator);
    Collection getOrCreateCollection(String tenant, String database, String name);
    Collection getOrCreateCollection(String tenant, String database, String name,
                                     Consumer<CreateCollectionRequest.Builder> configurator);
    Collection getCollection(String tenant, String database, String collectionId);
    List<Collection> listCollections(String tenant, String database);
    List<Collection> listCollections(String tenant, String database, Integer limit, Integer offset);
    int countCollections(String tenant, String database);
    void deleteCollection(String tenant, String database, String collectionId);
    void updateCollection(String tenant, String database, String collectionId,
                         Consumer<UpdateCollectionRequest.Builder> configurator);

    // Utility operations
    String heartbeat();
    String version();
    void reset();
}