package tech.amikos.chromadb;

/**
 * This exception encapsulates all exceptions thrown by the EmbeddingFunction class.
 */
public class EFException extends Exception {
    public EFException(String message) {
        super(message);
    }

    public EFException(String message, Throwable cause) {
        super(message, cause);
    }

    public EFException(Throwable cause) {
        super(cause);
    }
}
