package tech.amikos.chromadb.v2;

/** Client-side errors (HTTP 4xx). */
public class ChromaClientException extends ChromaException {

    public ChromaClientException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
