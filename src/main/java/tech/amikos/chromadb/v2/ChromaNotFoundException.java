package tech.amikos.chromadb.v2;

public class ChromaNotFoundException extends ChromaV2Exception {
    public ChromaNotFoundException(String message) {
        super(404, "NOT_FOUND", message);
    }
}