package tech.amikos.chromadb.v2.auth;

import okhttp3.Request;

class ChromaTokenAuthProvider implements AuthProvider {
    private final String token;

    ChromaTokenAuthProvider(String token) {
        this.token = token;
    }

    @Override
    public Request.Builder authenticate(Request.Builder requestBuilder) {
        return requestBuilder.header("X-Chroma-Token", token);
    }
}