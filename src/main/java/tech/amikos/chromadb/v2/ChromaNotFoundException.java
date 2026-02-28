package tech.amikos.chromadb.v2;

/** HTTP 404. */
public final class ChromaNotFoundException extends ChromaClientException {

    private static final String DEFAULT_ERROR_CODE = "NotFoundError";

    public ChromaNotFoundException(String message, String errorCode) {
        super(message, 404, errorCode != null ? errorCode : DEFAULT_ERROR_CODE);
    }

    public ChromaNotFoundException(String message) {
        super(message, 404, DEFAULT_ERROR_CODE);
    }

    public ChromaNotFoundException(String message, String errorCode, Throwable cause) {
        super(message, 404, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, cause);
    }
}
