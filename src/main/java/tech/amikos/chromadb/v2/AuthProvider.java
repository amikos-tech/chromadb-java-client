package tech.amikos.chromadb.v2;

import java.util.Map;

/** Applies authentication credentials to HTTP request headers. */
public interface AuthProvider {

    void applyAuth(Map<String, String> headers);
}
