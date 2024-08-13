package tech.amikos.chromadb;

public class ChromaException extends Exception {
    public ChromaException(String message) {
        super(message);
    }

    public ChromaException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChromaException(Throwable cause) {
        super(cause);
    }
}
