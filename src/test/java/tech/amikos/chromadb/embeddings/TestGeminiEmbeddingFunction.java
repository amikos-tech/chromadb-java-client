package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.gemini.GeminiEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @Test
    public void testEmbedQueryRejectsNullWithConfiguredModel() throws EFException {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );

        try {
            ef.embedQuery(null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-gemini-model"));
            assertTrue(e.getMessage().contains("query must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNullListWithConfiguredModel() throws EFException {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );

        try {
            ef.embedDocuments((List<String>) null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-gemini-model"));
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNullElementWithIndex() throws EFException {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );

        try {
            ef.embedDocuments(Arrays.asList("doc1", null));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-gemini-model"));
            assertTrue(e.getMessage().contains("document at index 1 must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsArrayRejectsNullWithConfiguredModel() throws EFException {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );

        try {
            ef.embedDocuments((String[]) null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("custom-gemini-model"));
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testToEmbeddingRejectsMissingEmbeddingsWithChromaException() throws Exception {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );
        Method method = GeminiEmbeddingFunction.class.getDeclaredMethod(
                "toEmbedding",
                com.google.genai.types.EmbedContentResponse.class,
                String.class
        );
        method.setAccessible(true);

        com.google.genai.types.EmbedContentResponse response = com.google.genai.types.EmbedContentResponse.builder()
                .build();

        try {
            method.invoke(ef, response, "custom-gemini-model");
            fail("Expected ChromaException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof ChromaException);
            assertTrue(e.getCause().getMessage().contains("Gemini returned no embeddings"));
        }
    }

    @Test
    public void testToEmbeddingRejectsMissingValuesWithChromaException() throws Exception {
        GeminiEmbeddingFunction ef = new GeminiEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.model("custom-gemini-model")
        );
        Method method = GeminiEmbeddingFunction.class.getDeclaredMethod(
                "toEmbedding",
                com.google.genai.types.EmbedContentResponse.class,
                String.class
        );
        method.setAccessible(true);

        com.google.genai.types.ContentEmbedding contentEmbedding = com.google.genai.types.ContentEmbedding.builder()
                .build();
        com.google.genai.types.EmbedContentResponse response = com.google.genai.types.EmbedContentResponse.builder()
                .embeddings(Collections.singletonList(contentEmbedding))
                .build();

        try {
            method.invoke(ef, response, "custom-gemini-model");
            fail("Expected ChromaException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof ChromaException);
            assertTrue(e.getCause().getMessage().contains("Gemini embedding has no values"));
        }
    }
}
