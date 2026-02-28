package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class AuthProviderTest {

    @Test
    public void testBasicAuthAppliesCorrectHeader() {
        BasicAuth auth = BasicAuth.of("user", "pass");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);

        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").startsWith("Basic "));
        // Base64("user:pass") = "dXNlcjpwYXNz"
        assertEquals("Basic dXNlcjpwYXNz", headers.get("Authorization"));
    }

    @Test(expected = NullPointerException.class)
    public void testBasicAuthRejectsNullUsername() {
        BasicAuth.of(null, "pass");
    }

    @Test(expected = NullPointerException.class)
    public void testBasicAuthRejectsNullPassword() {
        BasicAuth.of("user", null);
    }

    @Test
    public void testTokenAuthAppliesBearerHeader() {
        TokenAuth auth = TokenAuth.of("my-token");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);

        assertEquals("Bearer my-token", headers.get("Authorization"));
    }

    @Test(expected = NullPointerException.class)
    public void testTokenAuthRejectsNullToken() {
        TokenAuth.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenAuthRejectsEmptyToken() {
        TokenAuth.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenAuthRejectsBlankToken() {
        TokenAuth.of("   ");
    }

    @Test
    public void testChromaTokenAuthAppliesHeader() {
        ChromaTokenAuth auth = ChromaTokenAuth.of("chroma-tok");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);

        assertEquals("chroma-tok", headers.get("X-Chroma-Token"));
    }

    @Test(expected = NullPointerException.class)
    public void testChromaTokenAuthRejectsNullToken() {
        ChromaTokenAuth.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChromaTokenAuthRejectsEmptyToken() {
        ChromaTokenAuth.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChromaTokenAuthRejectsBlankToken() {
        ChromaTokenAuth.of("   ");
    }

    @Test
    public void testBasicAuthWithSpecialCharacters() {
        BasicAuth auth = BasicAuth.of("user@domain.com", "p@ss:w0rd!");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);

        assertTrue(headers.get("Authorization").startsWith("Basic "));
    }

    @Test
    public void testTokenAuthTrimsWhitespace() {
        TokenAuth auth = TokenAuth.of("  my-token  ");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);
        assertEquals("Bearer my-token", headers.get("Authorization"));
    }

    @Test
    public void testChromaTokenAuthTrimsWhitespace() {
        ChromaTokenAuth auth = ChromaTokenAuth.of("  chroma-tok  ");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        auth.applyAuth(headers);
        assertEquals("chroma-tok", headers.get("X-Chroma-Token"));
    }

    @Test(expected = IllegalStateException.class)
    public void testTokenAuthFromEnvThrowsWhenNotSet() {
        TokenAuth.fromEnv("NONEXISTENT_VAR_FOR_TEST_" + System.nanoTime());
    }

    @Test(expected = IllegalStateException.class)
    public void testChromaTokenAuthFromEnvThrowsWhenNotSet() {
        ChromaTokenAuth.fromEnv("NONEXISTENT_VAR_FOR_TEST_" + System.nanoTime());
    }

    @Test(expected = IllegalStateException.class)
    public void testBasicAuthFromEnvThrowsWhenUsernameNotSet() {
        BasicAuth.fromEnv("NONEXISTENT_VAR_FOR_TEST_" + System.nanoTime(), "PATH");
    }

    @Test(expected = IllegalStateException.class)
    public void testBasicAuthFromEnvThrowsWhenPasswordNotSet() {
        BasicAuth.fromEnv("PATH", "NONEXISTENT_VAR_FOR_TEST_" + System.nanoTime());
    }

    @Test
    public void testAuthProviderOverwritesPreviousHeader() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "old-value");

        TokenAuth.of("new-token").applyAuth(headers);
        assertEquals("Bearer new-token", headers.get("Authorization"));
    }
}
