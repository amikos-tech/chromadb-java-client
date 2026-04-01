package tech.amikos.chromadb.v2;

/**
 * Signals that a requested embedding provider is not registered.
 */
public final class UnsupportedEmbeddingProviderException extends ChromaException {

    public UnsupportedEmbeddingProviderException(String message) {
        super(message);
    }
}
