package tech.amikos.chromadb.v2;

import java.util.Map;
import java.util.Objects;

/** X-Chroma-Token header authentication (used by Chroma Cloud). */
public final class ChromaTokenAuth implements AuthProvider {

    private final String token;

    private ChromaTokenAuth(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    public static ChromaTokenAuth of(String token) {
        return new ChromaTokenAuth(token);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("X-Chroma-Token", token);
    }
}
