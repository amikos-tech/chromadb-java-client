package tech.amikos.chromadb.v2;

import okhttp3.Request;

public class NoAuthProvider implements AuthProvider {
    public static final NoAuthProvider INSTANCE = new NoAuthProvider();

    private NoAuthProvider() {}

    @Override
    public Request.Builder authenticate(Request.Builder requestBuilder) {
        return requestBuilder;
    }
}