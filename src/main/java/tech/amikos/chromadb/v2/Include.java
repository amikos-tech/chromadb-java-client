package tech.amikos.chromadb.v2;

/** Fields to include in query/get results. */
public enum Include {
    EMBEDDINGS("embeddings"),
    DOCUMENTS("documents"),
    METADATAS("metadatas"),
    DISTANCES("distances"),
    URIS("uris");

    private final String value;

    Include(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
