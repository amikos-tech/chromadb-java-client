package tech.amikos.chromadb.v2;

public class ChromaUnauthorizedException extends ChromaV2Exception {
    public ChromaUnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }
}