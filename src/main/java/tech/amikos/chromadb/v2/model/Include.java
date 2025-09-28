package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

public enum Include {
    @SerializedName("embeddings")
    EMBEDDINGS("embeddings"),

    @SerializedName("documents")
    DOCUMENTS("documents"),

    @SerializedName("metadatas")
    METADATAS("metadatas"),

    @SerializedName("distances")
    DISTANCES("distances"),

    @SerializedName("uris")
    URIS("uris");

    private final String value;

    Include(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}