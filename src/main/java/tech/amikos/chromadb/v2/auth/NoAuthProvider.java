package tech.amikos.chromadb.v2.auth;

import okhttp3.Request;

class NoAuthProvider implements AuthProvider {
    @Override
    public Request.Builder authenticate(Request.Builder requestBuilder) {
        return requestBuilder;
    }
}