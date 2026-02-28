package tech.amikos.chromadb.v2;

/** Client-side errors (HTTP 4xx). */
public class ChromaClientException extends ChromaException {

    public ChromaClientException(String message, int statusCode, String errorCode) {
        super(message, validateClientStatusCode(statusCode), errorCode);
    }

    public ChromaClientException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, validateClientStatusCode(statusCode), errorCode, cause);
    }

    private static int validateClientStatusCode(int statusCode) {
        if (statusCode < 400 || statusCode > 499) {
            throw new IllegalArgumentException("Client exception statusCode must be in [400, 499], got: " + statusCode);
        }
        return statusCode;
    }
}
