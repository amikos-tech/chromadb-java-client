package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

public class Tenant {
    @SerializedName("name")
    private final String name;

    public Tenant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}