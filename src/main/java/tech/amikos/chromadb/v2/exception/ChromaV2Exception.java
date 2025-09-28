package tech.amikos.chromadb.v2.exception;

public class ChromaV2Exception extends RuntimeException {
    private final int statusCode;
    private final String errorType;

    public ChromaV2Exception(String message) {
        super(message);
        this.statusCode = -1;
        this.errorType = "UNKNOWN";
    }

    public ChromaV2Exception(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorType = "UNKNOWN";
    }

    public ChromaV2Exception(int statusCode, String errorType, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }
}