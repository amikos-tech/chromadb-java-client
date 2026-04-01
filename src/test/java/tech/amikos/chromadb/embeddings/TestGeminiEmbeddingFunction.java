package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.gemini.GeminiEmbeddingFunction;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Unit tests for GeminiEmbeddingFunction.
 *
 * <p>These tests verify construction and configuration behavior.
 * Actual API calls require a real Gemini API key and are not tested here.</p>
 */
public class TestGeminiEmbeddingFunction {

    @Test
    public void testConstructWithApiKey() throws EFException {
        // Construction with explicit API key should not throw
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(WithParam.apiKey("test-key"));
        assertNotNull(ef);
    }

    @Test
    public void testDefaultModelName() throws Exception {
        Field field = GeminiEmbeddingFunction.class.getDeclaredField("DEFAULT_MODEL_NAME");
        field.setAccessible(true);
        assertEquals("text-embedding-004", field.get(null));
    }

    @Test
    public void testConstructWithCustomModel() throws EFException {
        // Construction with custom model should not throw
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-model")
        );
        assertNotNull(ef);
    }

    @Test
    public void testApiKeyFromEnvThrowsWhenNotSet() {
        // When GEMINI_API_KEY env var is not set, no-arg constructor should throw
        try {
            new GeminiEmbeddingFunction();
            fail("Expected EFException when GEMINI_API_KEY is not set");
        } catch (EFException e) {
            assertTrue(
                    "Expected message to mention GEMINI_API_KEY, got: " + e.getMessage(),
                    e.getMessage().contains("GEMINI_API_KEY")
            );
        }
    }

    @Test
    public void testGeminiApiKeyEnvConstant() {
        assertEquals("GEMINI_API_KEY", GeminiEmbeddingFunction.GEMINI_API_KEY_ENV);
    }
}
