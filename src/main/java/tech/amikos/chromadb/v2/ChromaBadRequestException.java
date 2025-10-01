package tech.amikos.chromadb.v2;

public class ChromaBadRequestException extends ChromaV2Exception {
    public ChromaBadRequestException(String message) {
        super(400, "BAD_REQUEST", message);
    }
}