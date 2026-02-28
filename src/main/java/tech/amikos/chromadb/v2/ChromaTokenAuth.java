package tech.amikos.chromadb.v2;

import java.util.Map;
import java.util.Objects;

/** X-Chroma-Token header authentication (used by Chroma Cloud). */
public final class ChromaTokenAuth implements AuthProvider {

    private final String token;

    private ChromaTokenAuth(String token) {
        this.token = validateToken(token);
    }

    /**
     * Creates Chroma Cloud token authentication.
     *
     * @throws NullPointerException     if {@code token} is {@code null}
     * @throws IllegalArgumentException if {@code token} is blank
     */
    public static ChromaTokenAuth of(String token) {
        return new ChromaTokenAuth(token);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("X-Chroma-Token", token);
    }

    private static String validateToken(String token) {
        String value = Objects.requireNonNull(token, "token");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return value;
    }
}
