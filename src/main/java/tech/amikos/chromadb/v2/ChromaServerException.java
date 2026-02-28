package tech.amikos.chromadb.v2;

/** Server-side errors (HTTP 5xx). */
public class ChromaServerException extends ChromaException {

    public ChromaServerException(String message, int statusCode, String errorCode) {
        super(message, validateServerStatusCode(statusCode), errorCode);
    }

    public ChromaServerException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, validateServerStatusCode(statusCode), errorCode, cause);
    }

    private static int validateServerStatusCode(int statusCode) {
        if (statusCode < 500 || statusCode > 599) {
            throw new IllegalArgumentException("Server exception statusCode must be in [500, 599], got: " + statusCode);
        }
        return statusCode;
    }
}
