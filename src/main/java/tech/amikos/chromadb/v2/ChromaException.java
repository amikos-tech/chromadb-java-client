package tech.amikos.chromadb.v2;

/** Base exception for all ChromaDB errors. Unchecked for fluent API ergonomics. */
public class ChromaException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public ChromaException(String message) {
        this(message, -1, null, null);
    }

    public ChromaException(String message, Throwable cause) {
        this(message, -1, null, cause);
    }

    public ChromaException(String message, int statusCode, String errorCode) {
        this(message, statusCode, errorCode, null);
    }

    public ChromaException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() { return statusCode; }
    public String getErrorCode() { return errorCode; }
}
