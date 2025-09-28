package tech.amikos.chromadb.v2;

import okhttp3.Credentials;
import okhttp3.Request;

class BasicAuthProvider implements AuthProvider {
    private final String credentials;

    BasicAuthProvider(String username, String password) {
        this.credentials = Credentials.basic(username, password);
    }

    @Override
    public Request.Builder authenticate(Request.Builder requestBuilder) {
        return requestBuilder.header("Authorization", credentials);
    }
}