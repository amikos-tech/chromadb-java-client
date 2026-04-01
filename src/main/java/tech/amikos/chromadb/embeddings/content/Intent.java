package tech.amikos.chromadb.embeddings.content;

import java.util.Locale;

/**
 * Embedding intent hint for providers that distinguish between document and query embeddings.
 */
public enum Intent {
    RETRIEVAL_DOCUMENT("retrieval_document"),
    RETRIEVAL_QUERY("retrieval_query"),
    CLASSIFICATION("classification"),
    CLUSTERING("clustering");

    private final String value;

    Intent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Intent fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Intent intent : values()) {
            if (intent.value.equals(normalized)) {
                return intent;
            }
        }
        throw new IllegalArgumentException("Unknown intent: " + value);
    }
}
