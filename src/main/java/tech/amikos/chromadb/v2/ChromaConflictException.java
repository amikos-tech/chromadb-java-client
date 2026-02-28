package tech.amikos.chromadb.v2;

/** HTTP 409. */
public final class ChromaConflictException extends ChromaClientException {

    public ChromaConflictException(String message, String errorCode) {
        super(message, 409, errorCode);
    }
}
