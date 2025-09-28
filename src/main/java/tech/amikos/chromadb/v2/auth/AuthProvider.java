package tech.amikos.chromadb.v2.auth;

import okhttp3.Request;

public interface AuthProvider {
    Request.Builder authenticate(Request.Builder requestBuilder);

    static AuthProvider none() {
        return new NoAuthProvider();
    }

    static AuthProvider token(String token) {
        return new TokenAuthProvider(token);
    }

    static AuthProvider basic(String username, String password) {
        return new BasicAuthProvider(username, password);
    }

    static AuthProvider chromaToken(String token) {
        return new ChromaTokenAuthProvider(token);
    }
}