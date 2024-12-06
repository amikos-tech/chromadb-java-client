package tech.amikos.chromadb.api;

public interface Collection {
    /**
     * The name of the collection.
     * @return the name of the collection
     */
    String name();

    /**
     * The unique identifier of the collection.
     * @return the unique identifier of the collection
     */
    String id();

    /**
     * The name of the tenant that owns the collection.
     * @return the name of the tenant that owns the collection
     */
    String tenant();

    /**
     * The name of the database that contains the collection.
     * @return the name of the database that contains the collection
     */
    String database();

    /**
     * The metadata of the collection.
     * @return the metadata of the collection
     */
    CollectionMetadata metadata();

    /**
     * The configuration of the collection.
     * @return the configuration of the collection
     */
    CollectionConfiguration configuration();

    /**
     * Adds a record to the collection.
     */
    void add();

    /**
     * Upserts a record in the collection.
     */
    void upsert();

    /**
     * Updates a record in the collection.
     */
    void update();

    /**
     * Deletes a record from the collection.
     */
    void delete();

    /**
     * Counts the number of records in the collection.
     * @return the number of records in the collection
     */
    long count();

    /**
     * Renames the collection.
     * @param newName the new name of the collection
     */
    void rename(String newName);

    /**
     * Updates the metadata of the collection.
     * @param newMetadata the new metadata of the collection
     */
    void updateMetadata(CollectionMetadata newMetadata);

    void get();
    void query();
}
