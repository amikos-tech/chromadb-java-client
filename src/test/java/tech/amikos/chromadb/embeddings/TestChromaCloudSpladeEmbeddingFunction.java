package tech.amikos.chromadb.embeddings;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.chromacloudsplade.ChromaCloudSpladeEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.SparseVector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Unit tests for ChromaCloudSpladeEmbeddingFunction using WireMock.
 */
public class TestChromaCloudSpladeEmbeddingFunction {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private String wireMockUrl() {
        return "http://localhost:" + wireMock.port() + "/api/v2/embed/splade";
    }

    private ChromaCloudSpladeEmbeddingFunction createFunction() throws EFException {
        return new ChromaCloudSpladeEmbeddingFunction(
                WithParam.apiKey("test-chroma-key"),
                WithParam.baseAPI(wireMockUrl())
        );
    }

    @Test
    public void testEmbedDocumentsSuccess() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"indices\":[1,5,10],\"values\":[0.5,0.3,0.1]}]}")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();
        List<SparseVector> result = ef.embedDocuments(Collections.singletonList("text"));

        assertEquals("Should return 1 vector", 1, result.size());
        assertArrayEquals("Indices should match", new int[]{1, 5, 10}, result.get(0).getIndices());
        assertArrayEquals("Values should match",
                new float[]{0.5f, 0.3f, 0.1f}, result.get(0).getValues(), 0.01f);
    }

    @Test
    public void testEmbedQuerySuccess() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"indices\":[2,7],\"values\":[0.9,0.4]}]}")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();
        SparseVector result = ef.embedQuery("test query");

        assertNotNull("Result should not be null", result);
        assertArrayEquals("Indices should match", new int[]{2, 7}, result.getIndices());
    }

    @Test
    public void testAuthHeader() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"indices\":[1],\"values\":[0.5]}]}")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();
        ef.embedDocuments(Collections.singletonList("text"));

        verify(postRequestedFor(urlEqualTo("/api/v2/embed/splade"))
                .withHeader("Authorization", equalTo("Bearer test-chroma-key")));
    }

    @Test
    public void testServerError() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal server error\"}")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();
        try {
            ef.embedDocuments(Collections.singletonList("text"));
            fail("Expected ChromaException for 500 response");
        } catch (ChromaException e) {
            assertTrue("Expected message to mention Splade, got: " + e.getMessage(),
                    e.getMessage().contains("Splade"));
        }
    }

    @Test
    public void testMultipleDocuments() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"indices\":[1,2],\"values\":[0.5,0.3]},{\"indices\":[3,4],\"values\":[0.8,0.2]}]}")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();
        List<SparseVector> result = ef.embedDocuments(Arrays.asList("doc1", "doc2"));

        assertEquals("Should return 2 vectors", 2, result.size());
        assertArrayEquals("First vector indices", new int[]{1, 2}, result.get(0).getIndices());
        assertArrayEquals("Second vector indices", new int[]{3, 4}, result.get(1).getIndices());
    }

    @Test
    public void testApiKeyFromEnvThrowsWhenNotSet() {
        try {
            new ChromaCloudSpladeEmbeddingFunction();
            fail("Expected EFException when CHROMA_API_KEY is not set");
        } catch (EFException e) {
            assertTrue("Expected message to mention CHROMA_API_KEY, got: " + e.getMessage(),
                    e.getMessage().contains("CHROMA_API_KEY"));
        }
    }

    @Test
    public void testEmbedQueryRejectsNull() throws EFException {
        ChromaCloudSpladeEmbeddingFunction ef = createFunction();

        try {
            ef.embedQuery(null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("query must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNull() throws EFException {
        ChromaCloudSpladeEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments((List<String>) null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsEmptyList() throws EFException {
        ChromaCloudSpladeEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments(Collections.<String>emptyList());
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("documents must not be empty"));
        }
    }

    @Test
    public void testMissingApiKeyFailsFast() throws EFException {
        ChromaCloudSpladeEmbeddingFunction ef = new ChromaCloudSpladeEmbeddingFunction(
                WithParam.baseAPI(wireMockUrl())
        );

        try {
            ef.embedDocuments(Collections.singletonList("text"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("API key must not be null or empty"));
        }
    }

    @Test
    public void testEmptyResponseBodyFailsDescriptively() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments(Collections.singletonList("text"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("response body was empty"));
        }
    }

    @Test
    public void testMalformedJsonFailsDescriptively() throws EFException {
        stubFor(post(urlEqualTo("/api/v2/embed/splade"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{not-json")));

        ChromaCloudSpladeEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments(Collections.singletonList("text"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Chroma Cloud Splade embedding failed"));
            assertNotNull(e.getCause());
        }
    }
}
