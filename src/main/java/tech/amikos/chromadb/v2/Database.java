package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Database {
    @SerializedName("id")
    private final UUID id;

    @SerializedName("name")
    private final String name;

    @SerializedName("tenant")
    private final String tenant;

    public Database(UUID id, String name, String tenant) {
        this.id = id;
        this.name = name;
        this.tenant = tenant;
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
}