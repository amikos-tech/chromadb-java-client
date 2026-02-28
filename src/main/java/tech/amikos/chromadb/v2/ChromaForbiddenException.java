package tech.amikos.chromadb.v2;

/** HTTP 403. */
public final class ChromaForbiddenException extends ChromaClientException {

    public ChromaForbiddenException(String message, String errorCode) {
        super(message, 403, errorCode);
    }

    public ChromaForbiddenException(String message, String errorCode, Throwable cause) {
        super(message, 403, errorCode, cause);
    }
}
