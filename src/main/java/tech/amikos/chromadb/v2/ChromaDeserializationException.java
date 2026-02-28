package tech.amikos.chromadb.v2;

/**
 * Indicates that a successful HTTP response could not be deserialized.
 */
public final class ChromaDeserializationException extends ChromaException {

    public ChromaDeserializationException(String message, int statusCode) {
        super(message, validateSuccessStatusCode(statusCode), null);
    }

    public ChromaDeserializationException(String message, int statusCode, Throwable cause) {
        super(message, validateSuccessStatusCode(statusCode), null, cause);
    }

    private static int validateSuccessStatusCode(int statusCode) {
        if (statusCode < 200 || statusCode > 299) {
            throw new IllegalArgumentException(
                    "Deserialization exception statusCode must be in [200, 299], got: " + statusCode
            );
        }
        return statusCode;
    }
}
