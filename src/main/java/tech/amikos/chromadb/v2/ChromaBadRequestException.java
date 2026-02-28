package tech.amikos.chromadb.v2;

/** HTTP 400. */
public final class ChromaBadRequestException extends ChromaClientException {

    public ChromaBadRequestException(String message, String errorCode) {
        super(message, 400, errorCode);
    }
}
