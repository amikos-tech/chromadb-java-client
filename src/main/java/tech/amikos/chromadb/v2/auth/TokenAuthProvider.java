package tech.amikos.chromadb.v2.auth;

import okhttp3.Request;

class TokenAuthProvider implements AuthProvider {
    private final String token;

    TokenAuthProvider(String token) {
        this.token = token;
    }

    @Override
    public Request.Builder authenticate(Request.Builder requestBuilder) {
        return requestBuilder.header("Authorization", "Bearer " + token);
    }
}