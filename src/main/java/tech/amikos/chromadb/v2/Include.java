package tech.amikos.chromadb.v2;

import java.util.Locale;

/** Fields to include in query/get results. */
public enum Include {
    /** Include embeddings in the response. */
    EMBEDDINGS("embeddings"),
    /** Include documents in the response. */
    DOCUMENTS("documents"),
    /** Include metadata objects in the response. */
    METADATAS("metadatas"),
    /** Only meaningful for query operations; ignored for get operations. */
    DISTANCES("distances"),
    /** Include URIs in the response. */
    URIS("uris");

    private final String value;

    Include(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Include fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Include include : values()) {
            if (include.value.equals(normalized)) {
                return include;
            }
        }
        throw new IllegalArgumentException("Unknown include value: " + value);
    }
}
