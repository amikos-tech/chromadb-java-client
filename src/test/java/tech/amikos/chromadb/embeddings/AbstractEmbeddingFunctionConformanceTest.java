package tech.amikos.chromadb.embeddings;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.v2.ChromaException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Abstract conformance test base class for all remote embedding function providers.
 *
 * <p>Subclasses implement provider-specific WireMock stubs and factory methods.
 * All shared conformance behaviors (null/empty rejection, error wrapping, count mismatch,
 * success flow) are tested here.</p>
 */
public abstract class AbstractEmbeddingFunctionConformanceTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    /**
     * Creates a provider embedding function configured to point at the WireMock base URL.
     *
     * @param baseUrl the WireMock base URL (e.g. "http://localhost:PORT")
     */
    protected abstract EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException;

    /**
     * Stubs a successful embedding response for the given number of input texts.
     *
     * @param inputCount number of input texts to return embeddings for
     */
    protected abstract void stubSuccess(int inputCount);

    /**
     * Stubs an HTTP error response.
     *
     * @param httpStatus HTTP status code
     * @param body response body
     */
    protected abstract void stubFailure(int httpStatus, String body);

    /**
     * Stubs a count-mismatch response: request sends {@code inputCount} texts,
     * response returns {@code returnCount} embeddings.
     *
     * @param inputCount number of texts in the request
     * @param returnCount number of embeddings returned
     */
    protected abstract void stubCountMismatch(int inputCount, int returnCount);

    /** Provider display name used in error message assertions (e.g., "OpenAI"). */
    protected abstract String providerName();

    /** Model name expected in error messages. */
    protected abstract String modelName();

    protected String baseUrl() {
        return "http://localhost:" + wireMock.port();
    }

    @Test
    public void testRejectsNullDocumentList() throws EFException {
        EmbeddingFunction ef = createEmbeddingFunction(baseUrl());
        try {
            ef.embedDocuments((List<String>) null);
            fail("Expected ChromaException for null document list");
        } catch (ChromaException e) {
            assertTrue(
                "Expected message to contain provider name '" + providerName() + "', got: " + e.getMessage(),
                e.getMessage().contains(providerName())
            );
        }
    }

    @Test
    public void testRejectsEmptyDocumentList() throws EFException {
        EmbeddingFunction ef = createEmbeddingFunction(baseUrl());
        try {
            ef.embedDocuments(Collections.<String>emptyList());
            fail("Expected ChromaException for empty document list");
        } catch (ChromaException e) {
            assertTrue(
                "Expected message to contain provider name '" + providerName() + "', got: " + e.getMessage(),
                e.getMessage().contains(providerName())
            );
        }
    }

    @Test
    public void testCountMismatchThrowsChromaException() throws EFException {
        stubCountMismatch(2, 1);
        EmbeddingFunction ef = createEmbeddingFunction(baseUrl());
        try {
            ef.embedDocuments(Arrays.asList("a", "b"));
            fail("Expected ChromaException for count mismatch");
        } catch (ChromaException e) {
            String msg = e.getMessage();
            assertTrue(
                "Expected message to contain '2', got: " + msg,
                msg.contains("2")
            );
            assertTrue(
                "Expected message to contain '1', got: " + msg,
                msg.contains("1")
            );
        }
    }

    @Test
    public void testProviderErrorWrappedAsChromaException() throws EFException {
        stubFailure(429, "{\"error\":\"rate limit exceeded\"}");
        EmbeddingFunction ef = createEmbeddingFunction(baseUrl());
        try {
            ef.embedDocuments(Collections.singletonList("test"));
            fail("Expected ChromaException on API failure");
        } catch (ChromaException e) {
            String msg = e.getMessage();
            assertTrue(
                "Expected message to contain provider name '" + providerName() + "', got: " + msg,
                msg.contains(providerName())
            );
            assertTrue(
                "Expected message to contain model name '" + modelName() + "', got: " + msg,
                msg.contains(modelName())
            );
        }
    }

    @Test
    public void testSuccessfulEmbedding() throws EFException {
        stubSuccess(2);
        EmbeddingFunction ef = createEmbeddingFunction(baseUrl());
        List<Embedding> result = ef.embedDocuments(Arrays.asList("hello", "world"));
        assertNotNull("Expected non-null result", result);
        assertEquals("Expected 2 embeddings for 2 inputs", 2, result.size());
    }
}
