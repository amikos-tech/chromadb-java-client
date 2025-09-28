package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.UUID;

/**
 * POJO representing collection metadata from the API.
 * This is the data model returned by the API, not the operational interface.
 */
public class CollectionModel {
    @SerializedName("id")
    private final UUID id;

    @SerializedName("name")
    private final String name;

    @SerializedName("tenant")
    private final String tenant;

    @SerializedName("database")
    private final String database;

    @SerializedName("metadata")
    private final Map<String, Object> metadata;

    @SerializedName("dimension")
    private final Integer dimension;

    @SerializedName("configuration_json")
    private final CollectionConfiguration configuration;

    @SerializedName("log_position")
    private final Long logPosition;

    @SerializedName("version")
    private final Integer version;

    public CollectionModel(UUID id, String name, String tenant, String database,
                          Map<String, Object> metadata, Integer dimension,
                          CollectionConfiguration configuration, Long logPosition, Integer version) {
        this.id = id;
        this.name = name;
        this.tenant = tenant;
        this.database = database;
        this.metadata = metadata;
        this.dimension = dimension;
        this.configuration = configuration;
        this.logPosition = logPosition;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTenant() {
        return tenant;
    }

    public String getDatabase() {
        return database;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Integer getDimension() {
        return dimension;
    }

    public CollectionConfiguration getConfiguration() {
        return configuration;
    }

    public Long getLogPosition() {
        return logPosition;
    }

    public Integer getVersion() {
        return version;
    }

    public String getResourceName() {
        return String.format("chroma://%s/%s/collections/%s", tenant, database, id);
    }
}