package tech.amikos.chromadb.v2.exception;

public class ChromaUnauthorizedException extends ChromaV2Exception {
    public ChromaUnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }
}