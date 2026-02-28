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
     * @throws IllegalArgumentException if {@code statusCode < 400} or {@code message} is blank
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
     * @throws IllegalArgumentException if {@code statusCode < 400} or {@code message} is blank
     * @return a {@link ChromaException} subclass matching the status code
     */
    public static ChromaException fromHttpResponse(int statusCode, String message, String errorCode, Throwable cause) {
        if (statusCode < 400) {
            throw new IllegalArgumentException("statusCode must be >= 400 for error responses, got: " + statusCode);
        }
        String validatedMessage = validateMessage(message);
        switch (statusCode) {
            case 400:
                return new ChromaBadRequestException(validatedMessage, errorCode, cause);
            case 401:
                return new ChromaUnauthorizedException(validatedMessage, errorCode, cause);
            case 403:
                return new ChromaForbiddenException(validatedMessage, errorCode, cause);
            case 404:
                return new ChromaNotFoundException(validatedMessage, errorCode, cause);
            case 409:
                return new ChromaConflictException(validatedMessage, errorCode, cause);
            default:
                if (statusCode >= 400 && statusCode < 500) {
                    return new ChromaClientException(validatedMessage, statusCode, errorCode, cause);
                } else if (statusCode >= 500) {
                    return new ChromaServerException(validatedMessage, statusCode, errorCode, cause);
                }
                throw new IllegalArgumentException("Unhandled HTTP status code: " + statusCode);
        }
    }

    private static String validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }
}
