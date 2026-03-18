package tech.amikos.chromadb.v2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** HTTP Basic authentication (Authorization: Basic base64(user:pass)). */
public final class BasicAuth implements AuthProvider {

    private final String encoded;

    private BasicAuth(String username, String password) {
        String validatedUsername = validateComponent("username", username);
        String validatedPassword = validateComponent("password", password);
        this.encoded = Base64.getEncoder()
                .encodeToString((validatedUsername + ":" + validatedPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates HTTP Basic authentication from username/password credentials.
     *
     * @param username basic auth username
     * @param password basic auth password
     * @throws NullPointerException     if {@code username} or {@code password} is {@code null}
     * @throws IllegalArgumentException if {@code username} or {@code password} is blank
     */
    public static BasicAuth of(String username, String password) {
        return new BasicAuth(username, password);
    }

    @Override
    public void applyAuth(Map<String, String> headers) {
        headers.put("Authorization", "Basic " + encoded);
    }

    /**
     * Creates HTTP Basic authentication from environment variables.
     *
     * @param usernameVar the name of the environment variable holding the username
     * @param passwordVar the name of the environment variable holding the password
     * @throws IllegalStateException    if either environment variable is not set
     * @throws IllegalArgumentException if either environment variable resolves to a blank value
     */
    public static BasicAuth fromEnv(String usernameVar, String passwordVar) {
        String username = System.getenv(usernameVar);
        if (username == null) {
            throw new IllegalStateException("Environment variable not set: " + usernameVar);
        }
        String password = System.getenv(passwordVar);
        if (password == null) {
            throw new IllegalStateException("Environment variable not set: " + passwordVar);
        }
        return new BasicAuth(username, password);
    }

    private static String validateComponent(String fieldName, String value) {
        String nonNullValue = Objects.requireNonNull(value, fieldName);
        if (nonNullValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return nonNullValue;
    }
}
