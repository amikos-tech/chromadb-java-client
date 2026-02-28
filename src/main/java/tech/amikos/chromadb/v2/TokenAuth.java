package tech.amikos.chromadb.v2;

import java.util.Map;
import java.util.Objects;

/** Bearer token authentication (Authorization: Bearer &lt;token&gt;). */
public final class TokenAuth implements AuthProvider {

    private final String token;

    private TokenAuth(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    public static TokenAuth of(String token) {
        return new TokenAuth(token);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("Authorization", "Bearer " + token);
    }
}
