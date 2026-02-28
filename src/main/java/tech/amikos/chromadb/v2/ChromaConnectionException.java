package tech.amikos.chromadb.v2;

/**
 * Network/timeout errors (connection refused, DNS failure, timeout).
 *
 * <p>No HTTP response was received, so {@link #getStatusCode()} returns
 * {@link ChromaException#STATUS_CODE_UNAVAILABLE}.</p>
 */
public final class ChromaConnectionException extends ChromaException {

    public ChromaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
