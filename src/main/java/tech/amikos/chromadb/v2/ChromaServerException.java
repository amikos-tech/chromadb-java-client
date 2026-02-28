package tech.amikos.chromadb.v2;

/** Server-side errors (HTTP 5xx). */
public class ChromaServerException extends ChromaException {

    public ChromaServerException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }

    public ChromaServerException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, statusCode, errorCode, cause);
    }
}
