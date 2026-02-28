package tech.amikos.chromadb.v2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** HTTP Basic authentication (Authorization: Basic base64(user:pass)). */
public final class BasicAuth implements AuthProvider {

    private final String encoded;

    private BasicAuth(String username, String password) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        this.encoded = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates HTTP Basic authentication from username/password credentials.
     *
     * @param username basic auth username
     * @param password basic auth password
     * @throws NullPointerException if {@code username} or {@code password} is {@code null}
     */
    public static BasicAuth of(String username, String password) {
        return new BasicAuth(username, password);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("Authorization", "Basic " + encoded);
    }
}
