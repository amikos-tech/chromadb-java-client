package tech.amikos.chromadb.v2;

import java.util.Map;
import java.util.Objects;

/** Bearer token authentication (Authorization: Bearer &lt;token&gt;). */
public final class TokenAuth implements AuthProvider {

    private final String token;

    private TokenAuth(String token) {
        this.token = validateToken(token);
    }

    /**
     * Creates bearer token authentication.
     *
     * @throws NullPointerException     if {@code token} is {@code null}
     * @throws IllegalArgumentException if {@code token} is blank
     */
    public static TokenAuth of(String token) {
        return new TokenAuth(token);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("Authorization", "Bearer " + token);
    }

    private static String validateToken(String token) {
        String value = Objects.requireNonNull(token, "token");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return value;
    }
}
