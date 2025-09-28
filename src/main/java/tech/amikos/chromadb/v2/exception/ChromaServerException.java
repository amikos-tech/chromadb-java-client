package tech.amikos.chromadb.v2.exception;

public class ChromaServerException extends ChromaV2Exception {
    public ChromaServerException(String message) {
        super(500, "SERVER_ERROR", message);
    }

    public ChromaServerException(int statusCode, String message) {
        super(statusCode, "SERVER_ERROR", message);
    }
}