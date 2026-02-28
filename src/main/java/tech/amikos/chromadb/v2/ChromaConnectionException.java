package tech.amikos.chromadb.v2;

/** Network/timeout errors (connection refused, DNS failure, timeout). */
public final class ChromaConnectionException extends ChromaException {

    public ChromaConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
