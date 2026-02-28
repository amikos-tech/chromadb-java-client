package tech.amikos.chromadb.v2;

/** HTTP 404. */
public final class ChromaNotFoundException extends ChromaClientException {

    public ChromaNotFoundException(String message, String errorCode) {
        super(message, 404, errorCode);
    }

    public ChromaNotFoundException(String message) {
        super(message, 404, "NotFoundError");
    }
}
