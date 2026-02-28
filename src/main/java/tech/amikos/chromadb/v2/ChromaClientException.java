package tech.amikos.chromadb.v2;

/** Client-side errors (HTTP 4xx). */
public class ChromaClientException extends ChromaException {

    public ChromaClientException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }

    public ChromaClientException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, statusCode, errorCode, cause);
    }
}
