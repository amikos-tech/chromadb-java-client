package tech.amikos.chromadb.v2;

/**
 * Base exception for all ChromaDB errors. Unchecked for fluent API ergonomics.
 *
 * <p>Use {@link ChromaExceptions#fromHttpResponse(int, String, String)} to map HTTP errors to
 * the appropriate subclass. Direct instantiation is reserved for subclasses.</p>
 */
public class ChromaException extends RuntimeException {

    /** Sentinel value used when an exception is not associated with an HTTP response status code. */
    public static final int STATUS_CODE_UNAVAILABLE = -1;

    private final int statusCode;
    private final String errorCode;

    protected ChromaException(String message) {
        this(message, STATUS_CODE_UNAVAILABLE, null, null);
    }

    protected ChromaException(String message, Throwable cause) {
        this(message, STATUS_CODE_UNAVAILABLE, null, cause);
    }

    protected ChromaException(String message, int statusCode, String errorCode) {
        this(message, statusCode, errorCode, null);
    }

    protected ChromaException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    /**
     * @return HTTP status code if available, otherwise {@link #STATUS_CODE_UNAVAILABLE}
     */
    public int getStatusCode() { return statusCode; }

    /**
     * @return true when {@link #getStatusCode()} represents a real HTTP status code
     */
    public boolean hasStatusCode() { return statusCode != STATUS_CODE_UNAVAILABLE; }

    public String getErrorCode() { return errorCode; }
}
