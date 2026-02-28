package tech.amikos.chromadb.v2;

/**
 * Factory for mapping HTTP responses to the appropriate {@link ChromaException} subclass.
 */
public final class ChromaExceptions {

    private ChromaExceptions() {}

    /**
     * Creates the appropriate exception for a given HTTP status code.
     *
     * @param statusCode HTTP status code
     * @param message    error message (typically from the response body)
     * @param errorCode  Chroma-specific error code, or {@code null}
     * @return a {@link ChromaException} subclass matching the status code
     */
    public static ChromaException fromHttpResponse(int statusCode, String message, String errorCode) {
        return fromHttpResponse(statusCode, message, errorCode, null);
    }

    /**
     * Creates the appropriate exception for a given HTTP status code, preserving a cause.
     *
     * @param statusCode HTTP status code
     * @param message    error message (typically from the response body)
     * @param errorCode  Chroma-specific error code, or {@code null}
     * @param cause      underlying cause, or {@code null}
     * @return a {@link ChromaException} subclass matching the status code
     */
    public static ChromaException fromHttpResponse(int statusCode, String message, String errorCode, Throwable cause) {
        switch (statusCode) {
            case 400:
                return new ChromaBadRequestException(message, errorCode, cause);
            case 401:
                return new ChromaUnauthorizedException(message, errorCode, cause);
            case 403:
                return new ChromaForbiddenException(message, errorCode, cause);
            case 404:
                return new ChromaNotFoundException(message, errorCode, cause);
            case 409:
                return new ChromaConflictException(message, errorCode, cause);
            default:
                if (statusCode >= 400 && statusCode < 500) {
                    return new ChromaClientException(message, statusCode, errorCode, cause);
                } else if (statusCode >= 500) {
                    return new ChromaServerException(message, statusCode, errorCode, cause);
                }
                return new ChromaException(message, statusCode, errorCode, cause);
        }
    }
}
