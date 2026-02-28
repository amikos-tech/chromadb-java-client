package tech.amikos.chromadb.v2;

/** HTTP 401. */
public final class ChromaUnauthorizedException extends ChromaClientException {

    public ChromaUnauthorizedException(String message, String errorCode) {
        super(message, 401, errorCode);
    }
}
